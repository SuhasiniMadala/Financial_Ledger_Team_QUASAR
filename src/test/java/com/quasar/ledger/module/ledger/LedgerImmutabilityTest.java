package com.quasar.ledger.module.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/** FR-LED-05, NFR-SAFE-06, DB-05, BR-10: the database itself refuses to change committed ledger data. */
class LedgerImmutabilityTest extends ApiTestBase {

    @Test
    @DisplayName("NFR-SAFE-06 (SRS 4.6.2 #3): direct SQL UPDATE/DELETE on entries and transactions is rejected")
    void directSqlChangesAreRejected() {
        TestUser asha = registerUser("Asha Rao");
        String wallet = createWallet(asha.token(), "INR");
        String txn = LedgerSteps.fundOk(api, adminToken(), wallet, 100_000);
        Map<String, Object> entryBefore = jdbc.queryForMap("SELECT * FROM ledger_entry WHERE transaction_id = ? AND account_id = ?",
                txn, wallet);

        assertThatThrownBy(() -> jdbc.update("UPDATE ledger_entry SET amount_minor = 1 WHERE transaction_id = ?", txn))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM ledger_entry WHERE transaction_id = ?", txn))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE transaction_record SET status = 'FAILED' WHERE id = ?", txn))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM transaction_record WHERE id = ?", txn))
                .isInstanceOf(DataAccessException.class);

        assertThat(jdbc.queryForMap("SELECT * FROM ledger_entry WHERE transaction_id = ? AND account_id = ?", txn, wallet))
                .isEqualTo(entryBefore);
    }

    @Test
    void walletStatusAuditIsAppendOnly() {
        TestUser asha = registerUser("Asha Rao");
        String wallet = createWallet(asha.token(), "INR");
        api.post("/api/v1/admin/wallets/" + wallet + "/freeze", Map.of("reason", "audit test"), adminToken());
        assertThatThrownBy(() -> jdbc.update("UPDATE wallet_status_change SET reason = 'x' WHERE account_id = ?", wallet))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM wallet_status_change WHERE account_id = ?", wallet))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("FR-LED-03: the database refuses entries for a FAILED transaction")
    void noEntriesForFailedTransactions() {
        TestUser asha = registerUser("Asha Rao");
        String wallet = createWallet(asha.token(), "INR");
        jdbc.update("INSERT INTO transaction_record (id, type, status, amount_minor, currency, initiated_by, source_account_id, "
                + "failure_code, created_at) VALUES ('T-FAILEDTEST01', 'TRANSFER', 'FAILED', 100, 'INR', ?, ?, 'INSUFFICIENT_FUNDS', "
                + "'2026-11-27T00:00:00.000000Z')", asha.accountId(), wallet);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO ledger_entry (id, transaction_id, account_id, direction, amount_minor, "
                + "balance_after_minor, created_at) VALUES ('E-FAILEDTEST01', 'T-FAILEDTEST01', ?, 'DEBIT', 100, 0, 'x')", wallet))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("FAILED");
    }
}
