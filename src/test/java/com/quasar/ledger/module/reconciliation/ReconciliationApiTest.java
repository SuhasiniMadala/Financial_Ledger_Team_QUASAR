package com.quasar.ledger.module.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.quasar.ledger.support.ApiClient;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SRS 4.7: reconciliation on a healthy ledger (FR-REC-01..08, NFR-SAFE-02). */
class ReconciliationApiTest extends ApiTestBase {

    @Test
    @DisplayName("FR-REC-01/07/08: PASS with per-currency totals, counts and zero discrepancies")
    void healthyLedgerPasses() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Asha Rao").token(), "INR");
        LedgerSteps.fundOk(api, admin, wallet, 925_000);
        Response r = LedgerSteps.reconcile(api, admin);
        assertThat(r.text("result")).isEqualTo("PASS");
        assertThat(r.text("runId")).startsWith("R-");
        assertThat(r.body().get("durationMs").asLong()).isNotNegative();
        JsonNode inr = null;
        for (JsonNode c : r.body().get("currencies")) {
            if ("INR".equals(c.get("currency").asText())) {
                inr = c;
            }
        }
        assertThat(inr).isNotNull();
        assertThat(inr.get("totalDebitsMinor").asLong()).isEqualTo(inr.get("totalCreditsMinor").asLong());
        assertThat(r.body().get("checked").get("entries").asLong()).isEqualTo(count("SELECT COUNT(*) FROM ledger_entry"));
        assertThat(r.body().get("checked").get("accounts").asLong()).isEqualTo(count("SELECT COUNT(*) FROM account"));
        assertThat(r.body().get("discrepancies")).isEmpty();
    }

    @Test
    @DisplayName("FR-REC-06: reconciliation changes no financial data")
    void reconciliationIsReadOnly() {
        String admin = adminToken();
        LedgerSteps.fundOk(api, admin, createWallet(registerUser("Read Only").token(), "INR"), 1_000);
        List<Map<String, Object>> accountsBefore = jdbc.queryForList("SELECT id, balance_minor, status FROM account ORDER BY id");
        long tx = count("SELECT COUNT(*) FROM transaction_record");
        long entries = count("SELECT COUNT(*) FROM ledger_entry");
        LedgerSteps.reconcile(api, admin);
        assertThat(jdbc.queryForList("SELECT id, balance_minor, status FROM account ORDER BY id")).isEqualTo(accountsBefore);
        assertThat(count("SELECT COUNT(*) FROM transaction_record")).isEqualTo(tx);
        assertThat(count("SELECT COUNT(*) FROM ledger_entry")).isEqualTo(entries);
    }

    @Test
    @DisplayName("FR-REC-05 (SRS 4.7.2 #3): transactions committing during reconciliation never cause false discrepancies")
    void snapshotIsConsistentUnderConcurrentWrites() throws Exception {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Busy").token(), "INR");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Response>> writes = new ArrayList<>();
            List<Future<Response>> audits = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                writes.add(pool.submit(() -> LedgerSteps.fund(new ApiClient("http://localhost:" + port, objectMapper), admin, wallet, 100,
                        newKey())));
                if (i % 4 == 0) {
                    audits.add(pool.submit(() -> new ApiClient("http://localhost:" + port, objectMapper)
                            .post("/api/v1/admin/reconciliations", null, admin)));
                }
            }
            for (Future<Response> w : writes) {
                assertThat(w.get().status()).isIn(201, 503);
            }
            for (Future<Response> a : audits) {
                assertThat(a.get().text("result")).as("%s", a.get()).isEqualTo("PASS");
            }
        } finally {
            pool.shutdownNow();
        }
        LedgerSteps.assertReconciles(api, admin);
    }
}
