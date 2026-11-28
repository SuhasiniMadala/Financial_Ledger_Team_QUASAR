package com.quasar.ledger.module.auth.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.id.IdGenerator;
import com.quasar.ledger.common.id.IdKind;
import com.quasar.ledger.common.security.Role;
import com.quasar.ledger.common.util.Timestamps;
import com.quasar.ledger.module.auth.dto.LoginResponse;
import com.quasar.ledger.module.auth.dto.RegisterMerchantRequest;
import com.quasar.ledger.module.auth.dto.RegisterUserRequest;
import com.quasar.ledger.module.auth.dto.RegistrationResponse;
import com.quasar.ledger.module.auth.model.MerchantProfile;
import com.quasar.ledger.module.auth.model.UserAccount;
import com.quasar.ledger.module.auth.repository.MerchantProfileRepository;
import com.quasar.ledger.module.auth.repository.UserAccountRepository;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.service.WalletService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and login (FR-AUTH-01..06). */
@Service
public class AuthService {

    static final String GENERIC_LOGIN_FAILURE = "Invalid email or password";
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final int MERCHANT_CODE_ATTEMPTS = 10;

    private final UserAccountRepository users;
    private final MerchantProfileRepository merchants;
    private final WalletService wallets;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final IdGenerator ids;
    private final Clock clock;
    /** Compared against when the email is unknown, so both failure cases take similar time (FR-AUTH-06). */
    private final String dummyHash;

    public AuthService(UserAccountRepository users, MerchantProfileRepository merchants, WalletService wallets,
                       PasswordEncoder passwordEncoder, JwtService jwt, IdGenerator ids, Clock clock) {
        this.users = users;
        this.merchants = merchants;
        this.wallets = wallets;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.ids = ids;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public RegistrationResponse registerUser(RegisterUserRequest request) {
        UserAccount account = createAccount(request.email(), request.password(), request.displayName().trim(), Role.USER);
        return new RegistrationResponse(account.id(), account.role().name(), account.email(), account.displayName(),
                account.createdAt(), null, null);
    }

    /** FR-AUTH-02 + FR-WAL-02: merchant account, unique merchant code and an ACTIVE INR wallet in one transaction. */
    @Transactional
    public RegistrationResponse registerMerchant(RegisterMerchantRequest request) {
        String businessName = request.businessName().trim();
        UserAccount account = createAccount(request.email(), request.password(), businessName, Role.MERCHANT);
        String code = newMerchantCode();
        merchants.insert(new MerchantProfile(ids.generate(IdKind.MERCHANT_PROFILE), account.id(), businessName, code));
        Account wallet = wallets.createMerchantWallet(account.id());
        return new RegistrationResponse(account.id(), account.role().name(), account.email(), account.displayName(),
                account.createdAt(), code, wallet.id());
    }

    /** Creates the configured ADMIN (FR-AUTH-05). Only called by {@link AdminBootstrap}, never by a controller. */
    @Transactional
    public void createConfiguredAdmin(String email, String password, String displayName) {
        createAccount(email, password, displayName, Role.ADMIN);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String password) {
        Optional<UserAccount> account = users.findByEmail(normalizeEmail(email));
        boolean ok;
        if (account.isPresent()) {
            ok = passwordEncoder.matches(password, account.get().passwordHash());
        } else {
            passwordEncoder.matches(password, dummyHash); // same work as a real check; result ignored
            ok = false;
        }
        if (!ok) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, GENERIC_LOGIN_FAILURE);
        }
        UserAccount a = account.get();
        JwtService.IssuedToken token = jwt.issue(a.id(), a.role());
        return new LoginResponse(token.token(), "Bearer", token.expiresInSeconds(), a.id(), a.role().name(), a.displayName());
    }

    private UserAccount createAccount(String rawEmail, String password, String displayName, Role role) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            // BCrypt only uses the first 72 bytes; reject rather than silently truncate (FR-AUTH-04).
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "password: must be at most 72 bytes");
        }
        String email = normalizeEmail(rawEmail);
        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "An account with this email already exists");
        }
        UserAccount account = new UserAccount(ids.generate(IdKind.USER), email, passwordEncoder.encode(password), displayName,
                role, Timestamps.format(clock.instant()));
        try {
            users.insert(account);
        } catch (DuplicateKeyException e) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "An account with this email already exists");
        }
        return account;
    }

    private String newMerchantCode() {
        for (int i = 0; i < MERCHANT_CODE_ATTEMPTS; i++) {
            String code = ids.generate(IdKind.MERCHANT_CODE);
            if (!merchants.existsByMerchantCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique merchant code");
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
