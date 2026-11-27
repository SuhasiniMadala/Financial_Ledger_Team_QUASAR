package com.quasar.ledger.module.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SRS 4.2.2 #3-#4: test funding (FR-WAL-07, FR-WAL-08 = idempotency rules FR-TRF-02..08, ACC-03, BR-02). */
class TestFundingApiTest extends ApiTestBase {

    @Test
    @DisplayName("SRS 4.2.2 #3: fund Rs 5,000.00, retry with the same key replays the original 201; posted once")
    void fundAndReplay() {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        String wallet = createWallet(asha.token(), "INR");
        long systemBefore = storedBalance("SYS-FUND-INR");
        String key = newKey();

        Response first = LedgerSteps.fund(api, admin, wallet, 500_000, key);
        assertThat(first.status()).isEqualTo(201);
        assertThat(first.text("type")).isEqualTo("TEST_FUNDING");
        assertThat(first.text("status")).isEqualTo("COMPLETED");
        assertThat(first.text("amountFormatted")).isEqualTo("₹5,000.00");
        assertThat(first.header("Idempotent-Replayed")).isNull();
        String txn = first.text("transactionId");

        Response retry = LedgerSteps.fund(api, admin, wallet, 500_000, key);
        assertThat(retry.status()).isEqualTo(201);
        assertThat(retry.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(retry.raw().body()).isEqualTo(first.raw().body());

        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE destination_account_id = ?", wallet)).isEqualTo(1);
        assertThat(storedBalance(wallet)).isEqualTo(500_000);
        assertThat(storedBalance("SYS-FUND-INR")).isEqualTo(systemBefore + 500_000);
        assertThat(jdbc.queryForList("SELECT account_id, direction, amount_minor FROM ledger_entry WHERE transaction_id = ? "
                + "ORDER BY sequence_no", txn)).containsExactly(
                Map.of("account_id", "SYS-FUND-INR", "direction", "DEBIT", "amount_minor", 500_000),
                Map.of("account_id", wallet, "direction", "CREDIT", "amount_minor", 500_000));
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("FR-TRF-06: same key with a different amount is 409 IDEMPOTENCY_KEY_REUSED with no effect")
    void sameKeyDifferentPayload() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Asha Rao").token(), "INR");
        String key = newKey();
        assertThat(LedgerSteps.fund(api, admin, wallet, 1_000, key).status()).isEqualTo(201);
        Response reused = LedgerSteps.fund(api, admin, wallet, 2_000, key);
        assertThat(reused.status()).isEqualTo(409);
        assertThat(reused.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(storedBalance(wallet)).isEqualTo(1_000);
    }

    @Test
    @DisplayName("FR-TRF-02/03: missing key is 400 IDEMPOTENCY_KEY_REQUIRED; malformed key is 400; nothing recorded")
    void keyIsRequiredAndValidated() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Asha Rao").token(), "INR");
        Response missing = LedgerSteps.fund(api, admin, wallet, 1_000, null);
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.code()).isEqualTo("IDEMPOTENCY_KEY_REQUIRED");
        assertThat(LedgerSteps.fund(api, admin, wallet, 1_000, "short").status()).isEqualTo(400);
        assertThat(LedgerSteps.fund(api, admin, wallet, 1_000, "has spaces in it").status()).isEqualTo(400);
        assertThat(storedBalance(wallet)).isZero();
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE destination_account_id = ?", wallet)).isZero();
    }

    @Test
    @DisplayName("BR-01/02 / TQ-6: amounts 0, -1, limit+1, 100.5 and \"100\" are 400; 1 and the limit are accepted")
    void amountBoundaries() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Asha Rao").token(), "INR");
        String path = "/api/v1/admin/wallets/" + wallet + "/test-funding";
        for (Object bad : new Object[] {0, -1, -50_000, 10_000_001, 100.5, "100", true}) {
            Response r = api.postIdempotent(path, Map.of("amountMinor", bad), admin, newKey());
            assertThat(r.status()).as("amount %s -> %s", bad, r).isEqualTo(400);
            assertThat(r.code()).isEqualTo("VALIDATION_ERROR");
        }
        assertThat(api.postIdempotent(path, Map.of(), admin, newKey()).status()).isEqualTo(400);
        assertThat(storedBalance(wallet)).isZero();
        assertThat(LedgerSteps.fund(api, admin, wallet, 1, newKey()).status()).isEqualTo(201);
        assertThat(LedgerSteps.fund(api, admin, wallet, 10_000_000, newKey()).status()).isEqualTo(201);
        assertThat(storedBalance(wallet)).isEqualTo(10_000_001);
    }

    @Test
    @DisplayName("SRS 4.2.2 #4: funding a FROZEN wallet is 422 WALLET_NOT_ACTIVE, no entries, and the 422 is replayed")
    void frozenWalletIsRejectedAndReplayed() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Ravi Kumar").token(), "INR");
        api.post("/api/v1/admin/wallets/" + wallet + "/freeze", Map.of("reason", "reported lost device"), admin);
        String key = newKey();
        Response r = LedgerSteps.fund(api, admin, wallet, 1_000, key);
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.code()).isEqualTo("WALLET_NOT_ACTIVE");
        assertThat(count("SELECT COUNT(*) FROM ledger_entry WHERE account_id = ?", wallet)).isZero();
        Response replay = LedgerSteps.fund(api, admin, wallet, 1_000, key);
        assertThat(replay.status()).isEqualTo(422);
        assertThat(replay.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(replay.raw().body()).isEqualTo(r.raw().body());
    }

    @Test
    void onlyUserWalletsCanBeFunded() {
        String admin = adminToken();
        TestMerchant shop = registerMerchant("Shop");
        Response merchant = LedgerSteps.fund(api, admin, shop.walletId(), 1_000, newKey());
        assertThat(merchant.status()).isEqualTo(422);
        assertThat(merchant.code()).isEqualTo("INVALID_DESTINATION");
        assertThat(LedgerSteps.fund(api, admin, "W-0000000000", 1_000, newKey()).status()).isEqualTo(404);
        assertThat(LedgerSteps.fund(api, admin, "SYS-FUND-INR", 1_000, newKey()).status()).isEqualTo(400);
    }

    @Test
    @DisplayName("ACC-01: a USD wallet is funded from the USD System Funding Account")
    void fundingUsesTheMatchingCurrencySystemAccount() {
        String admin = adminToken();
        String usd = createWallet(registerUser("Asha Rao").token(), "USD");
        long usdSystemBefore = storedBalance("SYS-FUND-USD");
        Response r = LedgerSteps.fund(api, admin, usd, 2_500, newKey());
        assertThat(r.text("currency")).isEqualTo("USD");
        assertThat(r.text("sourceAccountId")).isEqualTo("SYS-FUND-USD");
        assertThat(storedBalance("SYS-FUND-USD")).isEqualTo(usdSystemBefore + 2_500);
    }

    @Test
    @DisplayName("SRS 6.2 golden rule: per currency, System Funding balance = sum of wallet balances")
    void goldenRule() {
        LedgerSteps.fundOk(api, adminToken(), createWallet(registerUser("Golden").token(), "INR"), 12_345);
        for (String currency : new String[] {"INR", "USD"}) {
            long system = storedBalance("SYS-FUND-" + currency);
            long wallets = count("SELECT COALESCE(SUM(balance_minor), 0) FROM account WHERE currency = ? "
                    + "AND account_type <> 'SYSTEM_FUNDING'", currency);
            assertThat(system).as(currency).isEqualTo(wallets);
        }
    }
}
