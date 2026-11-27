package com.quasar.ledger.module.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import com.quasar.ledger.support.TransferSteps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SRS 4.3.2 scenarios 1-6 and FR-TRF-01..14 (scenario 7 is TransferRollbackTest). Start: W-1001 = Rs 5,000, W-1002 = Rs 2,000. */
class TransferApiTest extends ApiTestBase {

    private String admin;
    private TestUser asha;
    private TestUser ravi;
    private String ashaWallet;
    private String raviWallet;

    @BeforeEach
    void setUp() {
        admin = adminToken();
        asha = registerUser("Asha Rao");
        ravi = registerUser("Ravi Kumar");
        ashaWallet = createWallet(asha.token(), "INR");
        raviWallet = createWallet(ravi.token(), "INR");
        LedgerSteps.fundOk(api, admin, ashaWallet, 500_000);
        LedgerSteps.fundOk(api, admin, raviWallet, 200_000);
    }

    @Test
    @DisplayName("#1 success: Rs 1,000 -> COMPLETED, DEBIT source / CREDIT destination, balances Rs 4,000 / Rs 3,000, 201")
    void successfulTransfer() {
        Response r = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 100_000, newKey());
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.text("type")).isEqualTo("TRANSFER");
        assertThat(r.text("status")).isEqualTo("COMPLETED");
        String txn = r.text("transactionId");
        assertThat(txn).matches("^T-[0-9A-Z]{12}$");
        assertThat(storedBalance(ashaWallet)).isEqualTo(400_000);
        assertThat(storedBalance(raviWallet)).isEqualTo(300_000);
        List<Map<String, Object>> entries = jdbc.queryForList(
                "SELECT account_id, direction, amount_minor, balance_after_minor FROM ledger_entry WHERE transaction_id = ? "
                        + "ORDER BY sequence_no", txn);
        assertThat(entries).containsExactly(
                Map.of("account_id", ashaWallet, "direction", "DEBIT", "amount_minor", 100_000, "balance_after_minor", 400_000),
                Map.of("account_id", raviWallet, "direction", "CREDIT", "amount_minor", 100_000, "balance_after_minor", 300_000));
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("#2 duplicate key, same payload: original 201 replayed with Idempotent-Replayed: true; no new transaction")
    void replay() {
        String key = newKey();
        Response first = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 100_000, key);
        Response retry = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 100_000, key);
        assertThat(retry.status()).isEqualTo(201);
        assertThat(retry.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(retry.raw().body()).isEqualTo(first.raw().body());
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE type = 'TRANSFER' AND source_account_id = ?", ashaWallet))
                .isEqualTo(1);
        assertThat(storedBalance(ashaWallet)).isEqualTo(400_000);
    }

    @Test
    @DisplayName("#2b the same key belongs to the caller only: another user may use the same key independently")
    void keysAreScopedToTheCaller() {
        String key = newKey();
        assertThat(TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 1_000, key).status()).isEqualTo(201);
        Response ravis = TransferSteps.transfer(api, ravi.token(), raviWallet, ashaWallet, 1_000, key);
        assertThat(ravis.status()).isEqualTo(201);
        assertThat(ravis.header("Idempotent-Replayed")).isNull();
    }

    @Test
    @DisplayName("#3 duplicate key, different payload: 409 IDEMPOTENCY_KEY_REUSED, no effect")
    void reusedKey() {
        String key = newKey();
        TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 100_000, key);
        Response r = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 200_000, key);
        assertThat(r.status()).isEqualTo(409);
        assertThat(r.code()).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(storedBalance(ashaWallet)).isEqualTo(400_000);
    }

    @Test
    @DisplayName("#4 no header: 400 IDEMPOTENCY_KEY_REQUIRED, nothing recorded")
    void missingKey() {
        long before = count("SELECT COUNT(*) FROM transaction_record");
        Response r = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 100_000, null);
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.code()).isEqualTo("IDEMPOTENCY_KEY_REQUIRED");
        assertThat(count("SELECT COUNT(*) FROM transaction_record")).isEqualTo(before);
    }

    @Test
    @DisplayName("#5 insufficient balance: FAILED transaction, no entries, key saved, 422; retry replays the 422")
    void insufficientFunds() {
        String key = newKey();
        Response r = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 600_000, key);
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.code()).isEqualTo("INSUFFICIENT_FUNDS");
        String failed = r.text("transactionId");
        assertThat(jdbc.queryForMap("SELECT status, failure_code FROM transaction_record WHERE id = ?", failed))
                .containsEntry("status", "FAILED").containsEntry("failure_code", "INSUFFICIENT_FUNDS");
        assertThat(count("SELECT COUNT(*) FROM ledger_entry WHERE transaction_id = ?", failed)).isZero();
        assertThat(count("SELECT COUNT(*) FROM idempotency_record WHERE idem_key = ? AND response_status = 422", key)).isEqualTo(1);
        Response replay = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 600_000, key);
        assertThat(replay.status()).isEqualTo(422);
        assertThat(replay.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(replay.text("transactionId")).isEqualTo(failed);
        assertThat(storedBalance(ashaWallet)).isEqualTo(500_000);
    }

    @Test
    @DisplayName("#6 amounts -50000, 0, 100.5, \"100\", over the limit: 400 VALIDATION_ERROR, nothing recorded; exact balance works")
    void invalidAmounts() {
        long before = count("SELECT COUNT(*) FROM transaction_record");
        for (Object bad : new Object[] {-50_000, 0, 100.5, "100", 10_000_001, null}) {
            Response r = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, bad, newKey());
            assertThat(r.status()).as("amount %s -> %s", bad, r).isEqualTo(400);
            assertThat(r.code()).isEqualTo("VALIDATION_ERROR");
        }
        assertThat(count("SELECT COUNT(*) FROM transaction_record")).isEqualTo(before);
        // Boundary: spending exactly the whole balance leaves zero, never negative.
        assertThat(TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 500_000, newKey()).status()).isEqualTo(201);
        assertThat(storedBalance(ashaWallet)).isZero();
        assertThat(TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 1, newKey()).code()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    @DisplayName("FR-TRF-10: someone else's or a missing source wallet is 404 and records nothing")
    void sourceMustBeOwned() {
        long before = count("SELECT COUNT(*) FROM transaction_record");
        Response stolen = TransferSteps.transfer(api, asha.token(), raviWallet, ashaWallet, 1_000, newKey());
        Response missing = TransferSteps.transfer(api, asha.token(), "W-0000000000", raviWallet, 1_000, newKey());
        assertThat(stolen.status()).isEqualTo(404);
        assertThat(missing.status()).isEqualTo(404);
        assertThat(stolen.text("message")).isEqualTo(missing.text("message"));
        assertThat(count("SELECT COUNT(*) FROM transaction_record")).isEqualTo(before);
        assertThat(storedBalance(raviWallet)).isEqualTo(200_000);
    }

    @Test
    @DisplayName("FR-TRF-12/13 BR-04: merchant wallet, System Funding or unknown destination -> 422 INVALID_DESTINATION + FAILED")
    void invalidDestinations() {
        TestMerchant shop = registerMerchant("Shop");
        for (String dest : new String[] {shop.walletId(), "W-0000000000"}) {
            Response r = TransferSteps.transfer(api, asha.token(), ashaWallet, dest, 1_000, newKey());
            assertThat(r.status()).as(dest).isEqualTo(422);
            assertThat(r.code()).isEqualTo("INVALID_DESTINATION");
            assertThat(jdbc.queryForObject("SELECT status FROM transaction_record WHERE id = ?", String.class, r.text("transactionId")))
                    .isEqualTo("FAILED");
        }
        // The System Funding Account ID is not even a valid wallet ID.
        assertThat(TransferSteps.transfer(api, asha.token(), ashaWallet, "SYS-FUND-INR", 1_000, newKey()).status()).isEqualTo(400);
        assertThat(storedBalance(ashaWallet)).isEqualTo(500_000);
        assertThat(storedBalance(shop.walletId())).isZero();
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("BR-06 SAME_WALLET, BR-03 CURRENCY_MISMATCH and BR-07 WALLET_NOT_ACTIVE are 422 with a FAILED record")
    void otherBusinessRules() {
        assertThat(TransferSteps.transfer(api, asha.token(), ashaWallet, ashaWallet, 1_000, newKey()).code()).isEqualTo("SAME_WALLET");
        String raviUsd = createWallet(ravi.token(), "USD");
        assertThat(TransferSteps.transfer(api, asha.token(), ashaWallet, raviUsd, 1_000, newKey()).code()).isEqualTo("CURRENCY_MISMATCH");
        // SRS 4.8.2 #1: Ravi's wallet is frozen, Asha sends Rs 100 -> WALLET_NOT_ACTIVE.
        api.post("/api/v1/admin/wallets/" + raviWallet + "/freeze", Map.of("reason", "reported lost device"), admin);
        Response frozen = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 10_000, newKey());
        assertThat(frozen.status()).isEqualTo(422);
        assertThat(frozen.code()).isEqualTo("WALLET_NOT_ACTIVE");
        Response fromFrozen = TransferSteps.transfer(api, ravi.token(), raviWallet, ashaWallet, 10_000, newKey());
        assertThat(fromFrozen.code()).isEqualTo("WALLET_NOT_ACTIVE");
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE status = 'FAILED' AND initiated_by = ?", asha.accountId()))
                .isEqualTo(3);
        assertThat(storedBalance(ashaWallet)).isEqualTo(500_000);
        assertThat(storedBalance(raviWallet)).isEqualTo(200_000);
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("BR-07: a DEACTIVATED wallet can neither send nor receive -> 422 WALLET_NOT_ACTIVE + FAILED, balances unchanged")
    void deactivatedWalletsTakeNoPart() {
        Response deactivated = api.post("/api/v1/admin/wallets/" + raviWallet + "/deactivate",
                Map.of("reason", "account closed by user"), admin);
        assertThat(deactivated.text("status")).isEqualTo("DEACTIVATED");

        Response toDeactivated = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 10_000, newKey());
        assertThat(toDeactivated.status()).isEqualTo(422);
        assertThat(toDeactivated.code()).isEqualTo("WALLET_NOT_ACTIVE");
        Response fromDeactivated = TransferSteps.transfer(api, ravi.token(), raviWallet, ashaWallet, 10_000, newKey());
        assertThat(fromDeactivated.status()).isEqualTo(422);
        assertThat(fromDeactivated.code()).isEqualTo("WALLET_NOT_ACTIVE");

        for (Response r : List.of(toDeactivated, fromDeactivated)) {
            String failed = r.text("transactionId");
            assertThat(jdbc.queryForObject("SELECT status FROM transaction_record WHERE id = ?", String.class, failed))
                    .isEqualTo("FAILED");
            assertThat(count("SELECT COUNT(*) FROM ledger_entry WHERE transaction_id = ?", failed)).isZero();
        }
        assertThat(storedBalance(ashaWallet)).isEqualTo(500_000);
        assertThat(storedBalance(raviWallet)).isEqualTo(200_000);
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("FR-TRF-01: optional description up to 140 characters; malformed IDs are 400; merchants get 403")
    void descriptionIdsAndRoles() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceWalletId", ashaWallet);
        body.put("destinationWalletId", raviWallet);
        body.put("amountMinor", 1_000);
        body.put("description", "x".repeat(140));
        Response ok = api.postIdempotent("/api/v1/transfers", body, asha.token(), newKey());
        assertThat(ok.status()).isEqualTo(201);
        assertThat(ok.text("description")).hasSize(140);
        body.put("description", "x".repeat(141));
        assertThat(api.postIdempotent("/api/v1/transfers", body, asha.token(), newKey()).status()).isEqualTo(400);
        assertThat(TransferSteps.transfer(api, asha.token(), "nope", raviWallet, 1_000, newKey()).status()).isEqualTo(400);
        TestMerchant shop = registerMerchant("Campus Cafe");
        // SRS 4.4.2 #4: a merchant token calling POST /transfers is 403.
        assertThat(TransferSteps.transfer(api, shop.token(), shop.walletId(), raviWallet, 1_000, newKey()).status()).isEqualTo(403);
    }
}
