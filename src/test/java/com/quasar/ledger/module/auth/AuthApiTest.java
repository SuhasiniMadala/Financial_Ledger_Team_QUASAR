package com.quasar.ledger.module.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** SRS 4.1: registration, login, tokens (FR-AUTH-01..07, NFR-SEC-01, NFR-SEC-06). */
@ExtendWith(OutputCaptureExtension.class)
class AuthApiTest extends ApiTestBase {

    @Test
    @DisplayName("FR-AUTH-01: a visitor registers a USER account")
    void registersUser() {
        String email = uniqueEmail("asha");
        Response r = api.post("/api/v1/auth/register", Map.of("displayName", "Asha Rao", "email", email, "password", PASSWORD), null);
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.text("role")).isEqualTo("USER");
        assertThat(r.text("accountId")).startsWith("U-");
        assertThat(r.body().has("password")).isFalse();
        assertThat(r.body().has("passwordHash")).isFalse();
    }

    @Test
    @DisplayName("FR-AUTH-02 / FR-WAL-02: a merchant gets a unique code and an ACTIVE INR wallet with zero balance")
    void registersMerchantWithCodeAndWallet() {
        TestMerchant cafe = registerMerchant("Campus Cafe");
        assertThat(cafe.merchantCode()).matches("^MER-[0-9A-Z]{6}$");
        assertThat(cafe.walletId()).startsWith("W-");
        Map<String, Object> wallet = jdbc.queryForMap("SELECT * FROM account WHERE id = ?", cafe.walletId());
        assertThat(wallet).containsEntry("account_type", "MERCHANT_WALLET").containsEntry("currency", "INR")
                .containsEntry("status", "ACTIVE").containsEntry("owner_id", cafe.accountId());
        assertThat(storedBalance(cafe.walletId())).isZero();
        assertThat(count("SELECT COUNT(*) FROM ledger_entry WHERE account_id = ?", cafe.walletId())).isZero();
        assertThat(registerMerchant("Other Shop").merchantCode()).isNotEqualTo(cafe.merchantCode());
    }

    @Test
    @DisplayName("FR-AUTH-03: an existing email in any letter case gives 409 DUPLICATE_RESOURCE")
    void duplicateEmailIsRejectedCaseInsensitively() {
        TestUser ravi = registerUser("Ravi Kumar");
        Response again = api.post("/api/v1/auth/register",
                Map.of("displayName", "Ravi 2", "email", ravi.email().toUpperCase(Locale.ROOT), "password", PASSWORD), null);
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("DUPLICATE_RESOURCE");
        Response asMerchant = api.post("/api/v1/auth/register/merchant",
                Map.of("businessName", "Shop", "email", ravi.email(), "password", PASSWORD), null);
        assertThat(asMerchant.status()).isEqualTo(409);
    }

    @Test
    @DisplayName("FR-AUTH-04: passwords of 7 or 73 characters are rejected; 8 and 72 are accepted (boundaries)")
    void passwordLengthBoundaries() {
        assertThat(registerWithPassword("a".repeat(7)).status()).isEqualTo(400);
        assertThat(registerWithPassword("a".repeat(7)).code()).isEqualTo("VALIDATION_ERROR");
        assertThat(registerWithPassword("a".repeat(8)).status()).isEqualTo(201);
        assertThat(registerWithPassword("a".repeat(72)).status()).isEqualTo(201);
        assertThat(registerWithPassword("a".repeat(73)).status()).isEqualTo(400);
        // 72 characters but more than 72 bytes would be silently truncated by BCrypt - rejected instead.
        assertThat(registerWithPassword("₹".repeat(30)).status()).isEqualTo(400);
    }

    private Response registerWithPassword(String password) {
        return api.post("/api/v1/auth/register",
                Map.of("displayName", "Boundary", "email", uniqueEmail("pw"), "password", password), null);
    }

    @Test
    void invalidRegistrationInputIsRejected() {
        assertThat(api.post("/api/v1/auth/register", Map.of("displayName", "X", "email", "not-an-email", "password", PASSWORD), null)
                .status()).isEqualTo(400);
        assertThat(api.post("/api/v1/auth/register", Map.of("displayName", " ", "email", uniqueEmail("b"), "password", PASSWORD), null)
                .status()).isEqualTo(400);
        assertThat(api.post("/api/v1/auth/register", Map.of("displayName", "x".repeat(101), "email", uniqueEmail("c"),
                "password", PASSWORD), null).status()).isEqualTo(400);
    }

    @Test
    @DisplayName("FR-AUTH-05: the API cannot create an ADMIN (unknown 'role' field is rejected)")
    void cannotRegisterAdminThroughApi() {
        Response r = api.post("/api/v1/auth/register",
                Map.of("displayName", "Mallory", "email", uniqueEmail("m"), "password", PASSWORD, "role", "ADMIN"), null);
        assertThat(r.status()).isEqualTo(400);
        assertThat(count("SELECT COUNT(*) FROM user_account WHERE role = 'ADMIN'")).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-AUTH-05: the configured admin exists and can log in")
    void configuredAdminCanLogIn() {
        Response r = api.post("/api/v1/auth/login", Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD), null);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.text("role")).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("FR-AUTH-06: login returns a bearer JWT; wrong password and unknown email give the same 401")
    void loginSuccessAndGenericFailure() {
        TestUser asha = registerUser("Asha Rao");
        Response ok = api.post("/api/v1/auth/login", Map.of("email", asha.email().toUpperCase(Locale.ROOT), "password", PASSWORD), null);
        assertThat(ok.status()).isEqualTo(200);
        assertThat(ok.text("tokenType")).isEqualTo("Bearer");
        assertThat(ok.number("expiresInSeconds")).isEqualTo(3600);
        assertThat(ok.text("accessToken").split("\\.")).hasSize(3);

        Response wrongPassword = api.post("/api/v1/auth/login", Map.of("email", asha.email(), "password", "Wrong-password-1"), null);
        Response unknownEmail = api.post("/api/v1/auth/login", Map.of("email", uniqueEmail("ghost"), "password", PASSWORD), null);
        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(unknownEmail.status()).isEqualTo(401);
        assertThat(wrongPassword.code()).isEqualTo("UNAUTHENTICATED").isEqualTo(unknownEmail.code());
        assertThat(wrongPassword.text("message")).isEqualTo(unknownEmail.text("message"));
    }

    @Test
    @DisplayName("FR-AUTH-07: missing, malformed or forged tokens give 401")
    void badTokensAreRejected() {
        assertThat(api.get("/api/v1/wallets", null).status()).isEqualTo(401);
        assertThat(api.get("/api/v1/wallets", "garbage").status()).isEqualTo(401);
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJVLTEiLCJyb2xlIjoiQURNSU4ifQ.c2lnbmF0dXJl";
        Response r = api.get("/api/v1/admin/users", forged);
        assertThat(r.status()).isEqualTo(401);
        assertThat(r.code()).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    @DisplayName("NFR-SEC-01: passwords are stored only as BCrypt hashes with work factor >= 10")
    void passwordsAreBcryptHashed() {
        TestUser u = registerUser("Hash Check");
        String hash = jdbc.queryForObject("SELECT password_hash FROM user_account WHERE id = ?", String.class, u.accountId());
        assertThat(hash).startsWith("$2a$10$").doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("NFR-SEC-06: logs contain no passwords or tokens")
    void logsContainNoSecrets(CapturedOutput output) {
        TestUser u = registerUser("Log Check");
        api.post("/api/v1/auth/login", Map.of("email", u.email(), "password", "Wrong-password-1"), null);
        assertThat(output.getAll()).doesNotContain(PASSWORD).doesNotContain("Wrong-password-1").doesNotContain(u.token());
    }
}
