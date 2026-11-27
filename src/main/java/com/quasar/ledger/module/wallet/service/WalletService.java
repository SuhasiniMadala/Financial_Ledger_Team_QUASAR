package com.quasar.ledger.module.wallet.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.id.IdGenerator;
import com.quasar.ledger.common.id.IdKind;
import com.quasar.ledger.common.util.Timestamps;
import com.quasar.ledger.config.LedgerProperties;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountStatus;
import com.quasar.ledger.module.wallet.model.AccountType;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Wallet creation and owner-scoped reads (FR-WAL-01..06). */
@Service
public class WalletService {

    public static final String MERCHANT_CURRENCY = "INR";

    private final AccountRepository accounts;
    private final IdGenerator ids;
    private final Clock clock;
    private final List<String> supportedCurrencies;

    public WalletService(AccountRepository accounts, IdGenerator ids, Clock clock, LedgerProperties properties) {
        this.accounts = accounts;
        this.ids = ids;
        this.clock = clock;
        this.supportedCurrencies = properties.currencies();
    }

    public List<String> supportedCurrencies() {
        return supportedCurrencies;
    }

    /** FR-WAL-01/03/04: a USER creates at most one wallet per supported currency; it starts at zero and ACTIVE. */
    @Transactional
    public Account createUserWallet(String ownerId, String currency) {
        if (!supportedCurrencies.contains(currency)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Unsupported currency '" + currency + "'. Supported: " + String.join(", ", supportedCurrencies));
        }
        if (accounts.findByOwnerAndCurrency(ownerId, currency).isPresent()) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "You already have a " + currency + " wallet");
        }
        return insert(ownerId, AccountType.USER_WALLET, currency);
    }

    /** FR-WAL-02: every new merchant gets one ACTIVE INR wallet. Called inside the registration transaction. */
    public Account createMerchantWallet(String merchantAccountId) {
        return insert(merchantAccountId, AccountType.MERCHANT_WALLET, MERCHANT_CURRENCY);
    }

    @Transactional(readOnly = true)
    public List<Account> listOwn(String ownerId) {
        return accounts.findByOwner(ownerId);
    }

    /** FR-AUTH-09: a wallet that does not exist and a wallet owned by someone else look the same (404). */
    @Transactional(readOnly = true)
    public Account getOwn(String ownerId, String walletId) {
        return accounts.findById(walletId)
                .filter(a -> a.isOwnedBy(ownerId))
                .orElseThrow(() -> ApiException.notFound("Wallet"));
    }

    private Account insert(String ownerId, AccountType type, String currency) {
        Account account = new Account(ids.generate(IdKind.WALLET), ownerId, type, currency, 0L,
                AccountStatus.ACTIVE, Timestamps.format(clock.instant()));
        try {
            accounts.insert(account);
        } catch (DuplicateKeyException e) {
            // The UNIQUE(owner_id, currency) constraint is the final guard against a race (DB-04).
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "You already have a " + currency + " wallet");
        }
        return account;
    }
}
