package com.quasar.ledger.module.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.context.TestPropertySource;

/**
 * SRS 4.7.2 #2: a tester corrupts the database directly and reconciliation must report it (C1, C2, C3).
 * This class gets its OWN Spring context and database (distinct property) so the deliberate corruption
 * never affects other tests.
 */
@TestPropertySource(properties = "ledger.test.isolation=reconciliation-corruption")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReconciliationDetectsCorruptionTest extends ApiTestBase {

    @Test
    @Order(1)
    @DisplayName("C3 BALANCE_MISMATCH: stored Rs 2,000.00 edited to Rs 2,500.00 -> expected 200000, actual 250000, difference +50000")
    void detectsEditedBalance() {
        String admin = adminToken();
        String ravi = createWallet(registerUser("Ravi Kumar").token(), "INR");
        LedgerSteps.fundOk(api, admin, ravi, 200_000);
        LedgerSteps.assertReconciles(api, admin);

        jdbc.update("UPDATE account SET balance_minor = 250000 WHERE id = ?", ravi);
        Response r = LedgerSteps.reconcile(api, admin);
        assertThat(r.text("result")).isEqualTo("FAIL");
        JsonNode d = find(r, "BALANCE_MISMATCH", ravi);
        assertThat(d.get("expectedMinor").asLong()).isEqualTo(200_000);
        assertThat(d.get("actualMinor").asLong()).isEqualTo(250_000);
        assertThat(d.get("differenceMinor").asLong()).isEqualTo(50_000);
        // FR-REC-06: it reports but never repairs.
        assertThat(storedBalance(ravi)).isEqualTo(250_000);

        jdbc.update("UPDATE account SET balance_minor = 200000 WHERE id = ?", ravi);
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @Order(2) // leaves the database deliberately corrupted, so it must run last
    @DisplayName("C2 TRANSACTION_IMBALANCE and C1 CURRENCY_IMBALANCE: a one-sided COMPLETED transaction is detected")
    void detectsUnbalancedTransaction() {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        String wallet = createWallet(asha.token(), "INR");
        jdbc.update("INSERT INTO transaction_record (id, type, status, amount_minor, currency, initiated_by, source_account_id, "
                + "destination_account_id, created_at) VALUES ('T-CORRUPTED001', 'TRANSFER', 'COMPLETED', 500, 'INR', ?, ?, ?, "
                + "'2026-11-27T00:00:00.000000Z')", asha.accountId(), wallet, wallet);
        jdbc.update("INSERT INTO ledger_entry (id, transaction_id, account_id, direction, amount_minor, balance_after_minor, "
                + "created_at) VALUES ('E-CORRUPTED001', 'T-CORRUPTED001', ?, 'CREDIT', 500, 500, '2026-11-27T00:00:00.000000Z')", wallet);

        Response r = LedgerSteps.reconcile(api, admin);
        assertThat(r.text("result")).isEqualTo("FAIL");
        assertThat(find(r, "TRANSACTION_IMBALANCE", "T-CORRUPTED001").get("differenceMinor").asLong()).isEqualTo(500);
        assertThat(find(r, "CURRENCY_IMBALANCE", "INR").get("differenceMinor").asLong()).isEqualTo(500);
        assertThat(find(r, "BALANCE_MISMATCH", wallet).get("expectedMinor").asLong()).isEqualTo(500);
        assertThat(api.get("/api/v1/admin/statistics", admin).body().get("latestReconciliation").get("result").asText())
                .isEqualTo("FAIL");
    }

    private static JsonNode find(Response report, String type, String subject) {
        for (JsonNode d : report.body().get("discrepancies")) {
            if (type.equals(d.get("type").asText()) && subject.equals(d.get("subjectId").asText())) {
                return d;
            }
        }
        throw new AssertionError("No " + type + " for " + subject + " in " + report);
    }
}
