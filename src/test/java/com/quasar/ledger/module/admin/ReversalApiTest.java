package com.quasar.ledger.module.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.module.ledger.model.NewTransaction;
import com.quasar.ledger.module.ledger.model.Posting;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.service.LedgerService;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** SRS 4.6.2 #4-#5: reversals (FR-LED-07..10, ACC-04, BR-07, BR-10). */
class ReversalApiTest extends ApiTestBase {

    private static final String REASON = "sent to wrong recipient";

    @Autowired
    private LedgerService ledger;

    @Autowired
    private TransactionTemplate writeTransaction;

    @Test
    @DisplayName("ACC-04: a reversal posts mirror-image entries, links to the original and leaves the original unchanged")
    void reversalMirrorsTheOriginal() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Asha Rao").token(), "INR");
        String original = LedgerSteps.fundOk(api, admin, wallet, 100_000);
        Map<String, Object> originalRow = jdbc.queryForMap("SELECT * FROM transaction_record WHERE id = ?", original);

        Response r = LedgerSteps.reverse(api, admin, original, REASON, newKey());
        assertThat(r.status()).as("%s", r).isEqualTo(201);
        assertThat(r.text("type")).isEqualTo("REVERSAL");
        assertThat(r.text("reversalOfId")).isEqualTo(original);
        assertThat(r.text("description")).isEqualTo(REASON);
        String reversal = r.text("transactionId");

        assertThat(storedBalance(wallet)).isZero();
        assertThat(jdbc.queryForList("SELECT account_id, direction FROM ledger_entry WHERE transaction_id = ? ORDER BY sequence_no",
                reversal)).containsExactly(Map.of("account_id", "SYS-FUND-INR", "direction", "CREDIT"),
                Map.of("account_id", wallet, "direction", "DEBIT"));
        assertThat(jdbc.queryForMap("SELECT * FROM transaction_record WHERE id = ?", original)).isEqualTo(originalRow);
        Response detail = api.get("/api/v1/admin/transactions/" + original, admin);
        assertThat(detail.body().get("transaction").get("reversedBy").asText()).isEqualTo(reversal);
        LedgerSteps.assertReconciles(api, admin);
    }

  
@Test
@DisplayName("SRS 4.6.2 #5: reversing again is 409 ALREADY_REVERSED; reversing a REVERSAL or a FAILED txn is 422 NOT_REVERSIBLE")
void cannotReverseTwiceOrReverseReversals() {
    String admin = adminToken();
    TestUser asha = registerUser("Asha Rao");
    String wallet = createWallet(asha.token(), "INR");

    String original = LedgerSteps.fundOk(api, admin, wallet, 100_000);
    String reversal = LedgerSteps.reverse(api, admin, original, REASON, newKey())
            .text("transactionId");

    // An already-reversed transaction cannot be reversed again.
    Response again = LedgerSteps.reverse(api, admin, original, REASON, newKey());
    assertThat(again.status()).isEqualTo(409);
    assertThat(again.code()).isEqualTo("ALREADY_REVERSED");

    // A reversal transaction cannot itself be reversed.
    Response ofReversal = LedgerSteps.reverse(api, admin, reversal, REASON, newKey());
    assertThat(ofReversal.status()).isEqualTo(422);
    assertThat(ofReversal.code()).isEqualTo("NOT_REVERSIBLE");

    // Create a FAILED transaction.
    String failed = writeTransaction.execute(s -> ledger.recordFailed(
            new NewTransaction(
                    TransactionType.TRANSFER,
                    5_000,
                    "INR",
                    asha.accountId(),
                    wallet,
                    null,
                    null,
                    null),
            com.quasar.ledger.common.api.ErrorCode.INSUFFICIENT_FUNDS)).id();

    // Record database counts before attempting the reversal.
    int transactionsBefore = jdbc.queryForObject(
            "SELECT COUNT(*) FROM transaction_record", Integer.class);
    int entriesBefore = jdbc.queryForObject(
            "SELECT COUNT(*) FROM ledger_entry", Integer.class);

    // Attempt to reverse the FAILED transaction.
    Response failedReversal = LedgerSteps.reverse(
            api, admin, failed, REASON, newKey());

    // Verify that the API rejects the request.
    assertThat(failedReversal.status()).isEqualTo(422);
    assertThat(failedReversal.code()).isEqualTo("NOT_REVERSIBLE");

    // Verify that no reversal transaction was created.
    assertThat(count(
            "SELECT COUNT(*) FROM transaction_record WHERE reversal_of_id = ?",
            failed)).isZero();

    // Verify that no new transaction or ledger entries were created.
    int transactionsAfter = jdbc.queryForObject(
            "SELECT COUNT(*) FROM transaction_record", Integer.class);
    int entriesAfter = jdbc.queryForObject(
            "SELECT COUNT(*) FROM ledger_entry", Integer.class);

    assertThat(transactionsAfter).isEqualTo(transactionsBefore);
    assertThat(entriesAfter).isEqualTo(entriesBefore);

    // The original transaction should still have exactly one reversal.
    assertThat(count(
            "SELECT COUNT(*) FROM transaction_record WHERE reversal_of_id = ?",
            original)).isEqualTo(1);
}

    @Test
    @DisplayName("FR-LED-08: reason must be 10-500 characters; key required; replay works")
    void reasonAndIdempotency() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Asha Rao").token(), "INR");
        String original = LedgerSteps.fundOk(api, admin, wallet, 1_000);
        assertThat(LedgerSteps.reverse(api, admin, original, "too short", newKey()).status()).isEqualTo(400);
        assertThat(LedgerSteps.reverse(api, admin, original, "x".repeat(501), newKey()).status()).isEqualTo(400);
        assertThat(LedgerSteps.reverse(api, admin, original, REASON, null).code()).isEqualTo("IDEMPOTENCY_KEY_REQUIRED");
        String key = newKey();
        Response first = LedgerSteps.reverse(api, admin, original, "exactly10!", key);
        assertThat(first.status()).isEqualTo(201);
        Response replay = LedgerSteps.reverse(api, admin, original, "exactly10!", key);
        assertThat(replay.status()).isEqualTo(201);
        assertThat(replay.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(LedgerSteps.reverse(api, admin, "T-000000000000", REASON, newKey()).status()).isEqualTo(404);
        assertThat(LedgerSteps.reverse(api, admin, "bad-id", REASON, newKey()).status()).isEqualTo(400);
    }

    @Test
    @DisplayName("BR-07 / FR-LED-10: FROZEN wallets can be reversed; DEACTIVATED wallets give 422 WALLET_NOT_ACTIVE")
    void walletStatusRules() {
        String admin = adminToken();
        String frozen = createWallet(registerUser("Frozen").token(), "INR");
        String frozenTxn = LedgerSteps.fundOk(api, admin, frozen, 1_000);
        api.post("/api/v1/admin/wallets/" + frozen + "/freeze", Map.of("reason", "investigation"), admin);
        assertThat(LedgerSteps.reverse(api, admin, frozenTxn, REASON, newKey()).status()).isEqualTo(201);

        String closed = createWallet(registerUser("Closed").token(), "INR");
        String closedTxn = LedgerSteps.fundOk(api, admin, closed, 1_000);
        api.post("/api/v1/admin/wallets/" + closed + "/deactivate", Map.of("reason", "account closed"), admin);
        Response r = LedgerSteps.reverse(api, admin, closedTxn, REASON, newKey());
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.code()).isEqualTo("WALLET_NOT_ACTIVE");
        assertThat(storedBalance(closed)).isEqualTo(1_000);
    }

    @Test
    @DisplayName("FR-LED-10: a reversal that would make a wallet negative is 422 INSUFFICIENT_FUNDS")
    void reversalCannotOverdraw() {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        TestUser ravi = registerUser("Ravi Kumar");
        String a = createWallet(asha.token(), "INR");
        String b = createWallet(ravi.token(), "INR");
        String funding = LedgerSteps.fundOk(api, admin, a, 10_000);
        // Asha spends 6,000 of it (posted straight through the ledger engine; transfers are tested in the transfer module).
        writeTransaction.executeWithoutResult(s -> ledger.postCompleted(new NewTransaction(TransactionType.TRANSFER, 6_000, "INR",
                asha.accountId(), a, b, null, null), List.of(Posting.debit(a, 6_000), Posting.credit(b, 6_000))));
        Response r = LedgerSteps.reverse(api, admin, funding, REASON, newKey());
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.code()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(storedBalance(a)).isEqualTo(4_000);
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE reversal_of_id = ?", funding)).isZero();
    }
}
