package com.quasar.ledger.module.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SRS 4.2: wallet creation and owner-scoped reads (FR-WAL-01, 03, 04, 06; FR-AUTH-09). */
class WalletApiTest extends ApiTestBase {

    @Test
    @DisplayName("FR-WAL-01/04: a new wallet is ACTIVE, zero balance, no ledger entries")
    void createsWallet() {
        TestUser asha = registerUser("Asha Rao");
        Response r = api.post("/api/v1/wallets", Map.of("currency", "INR"), asha.token());
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.text("currency")).isEqualTo("INR");
        assertThat(r.number("balanceMinor")).isZero();
        assertThat(r.text("balanceFormatted")).isEqualTo("₹0.00");
        assertThat(r.text("status")).isEqualTo("ACTIVE");
        String id = r.text("walletId");
        assertThat(id).matches("^W-[0-9A-Z]{10}$");
        assertThat(storedBalance(id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM ledger_entry WHERE account_id = ?", id)).isZero();
        assertThat(jdbc.queryForObject("SELECT account_type FROM account WHERE id = ?", String.class, id)).isEqualTo("USER_WALLET");
    }

    @Test
    @DisplayName("FR-WAL-03: second wallet in the same currency is 409; unsupported currency is 400")
    void oneWalletPerCurrencyAndSupportedCurrenciesOnly() {
        TestUser asha = registerUser("Asha Rao");
        createWallet(asha.token(), "INR");
        Response second = api.post("/api/v1/wallets", Map.of("currency", "INR"), asha.token());
        assertThat(second.status()).isEqualTo(409);
        assertThat(second.code()).isEqualTo("DUPLICATE_RESOURCE");

        Response xyz = api.post("/api/v1/wallets", Map.of("currency", "XYZ"), asha.token());
        assertThat(xyz.status()).isEqualTo(400);
        assertThat(xyz.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(api.post("/api/v1/wallets", Map.of("currency", "inr"), asha.token()).status()).isEqualTo(400);
        assertThat(api.post("/api/v1/wallets", Map.of(), asha.token()).status()).isEqualTo(400);

        // A different supported currency is allowed (TBD-1 / ADR-008).
        assertThat(api.post("/api/v1/wallets", Map.of("currency", "USD"), asha.token()).status()).isEqualTo(201);
    }

    @Test
    @DisplayName("FR-WAL-06: a user lists only their own wallets")
    void listsOwnWalletsOnly() {
        TestUser asha = registerUser("Asha Rao");
        TestUser ravi = registerUser("Ravi Kumar");
        String inr = createWallet(asha.token(), "INR");
        String usd = createWallet(asha.token(), "USD");
        createWallet(ravi.token(), "INR");
        Response r = api.get("/api/v1/wallets", asha.token());
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).hasSize(2);
        assertThat(r.body().findValuesAsText("walletId")).containsExactlyInAnyOrder(inr, usd);
    }

    @Test
    @DisplayName("FR-AUTH-09: another user's wallet returns 404 and does not reveal it exists")
    void otherUsersWalletIsNotFound() {
        TestUser asha = registerUser("Asha Rao");
        TestUser ravi = registerUser("Ravi Kumar");
        String raviWallet = createWallet(ravi.token(), "INR");
        Response r = api.get("/api/v1/wallets/" + raviWallet, asha.token());
        Response missing = api.get("/api/v1/wallets/W-0000000000", asha.token());
        assertThat(r.status()).isEqualTo(404);
        assertThat(missing.status()).isEqualTo(404);
        assertThat(r.text("message")).isEqualTo(missing.text("message"));
        assertThat(api.get("/api/v1/wallets/" + raviWallet, ravi.token()).status()).isEqualTo(200);
    }

    @Test
    void malformedWalletIdIs400() {
        TestUser asha = registerUser("Asha Rao");
        Response r = api.get("/api/v1/wallets/not-a-wallet", asha.token());
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("FR-AUTH-08: a merchant cannot use the USER wallet endpoints")
    void merchantCannotUseUserWalletEndpoints() {
        TestMerchant shop = registerMerchant("Shop");
        Response r = api.post("/api/v1/wallets", Map.of("currency", "USD"), shop.token());
        assertThat(r.status()).isEqualTo(403);
        assertThat(r.code()).isEqualTo("FORBIDDEN");
    }
}
