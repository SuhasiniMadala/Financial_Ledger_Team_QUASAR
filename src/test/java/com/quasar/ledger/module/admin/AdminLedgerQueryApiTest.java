package com.quasar.ledger.module.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-ADM-03 (transactions and entries) and FR-ADM-07 (statistics). */
class AdminLedgerQueryApiTest extends ApiTestBase {

    @Test
    @DisplayName("FR-ADM-03: list transactions filtered by wallet, type, status and date; newest first")
    void listAndFilter() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Asha Rao").token(), "INR");
        String first = LedgerSteps.fundOk(api, admin, wallet, 1_000);
        String second = LedgerSteps.fundOk(api, admin, wallet, 2_000);

        Response r = api.get("/api/v1/admin/transactions?walletId=" + wallet, admin);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("totalItems").asLong()).isEqualTo(2);
        assertThat(r.body().get("items").get(0).get("transactionId").asText()).isEqualTo(second);
        assertThat(r.body().get("items").get(1).get("transactionId").asText()).isEqualTo(first);

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        assertThat(api.get("/api/v1/admin/transactions?walletId=" + wallet + "&type=TEST_FUNDING&status=COMPLETED&from=" + today
                + "&to=" + today, admin).body().get("totalItems").asLong()).isEqualTo(2);
        assertThat(api.get("/api/v1/admin/transactions?walletId=" + wallet + "&type=TRANSFER", admin).body().get("totalItems").asLong())
                .isZero();
        assertThat(api.get("/api/v1/admin/transactions?walletId=" + wallet + "&from=2000-01-01&to=2000-01-02", admin)
                .body().get("totalItems").asLong()).isZero();
        assertThat(api.get("/api/v1/admin/transactions?type=NOPE", admin).status()).isEqualTo(400);
        assertThat(api.get("/api/v1/admin/transactions?from=27-11-2026", admin).status()).isEqualTo(400);
        assertThat(api.get("/api/v1/admin/transactions?from=2026-11-27&to=2026-11-01", admin).status()).isEqualTo(400);
    }

    @Test
    @DisplayName("FR-ADM-03: view the entries of a transaction and of an account (System Funding included)")
    void entriesOfTransactionAndAccount() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Asha Rao").token(), "INR");
        String txn = LedgerSteps.fundOk(api, admin, wallet, 7_000);
        Response detail = api.get("/api/v1/admin/transactions/" + txn, admin);
        assertThat(detail.body().get("entries")).hasSize(2);
        JsonNode credit = detail.body().get("entries").get(1);
        assertThat(credit.get("accountId").asText()).isEqualTo(wallet);
        assertThat(credit.get("balanceAfterMinor").asLong()).isEqualTo(7_000);
        assertThat(credit.get("sequenceNo").asLong()).isPositive();

        Response entries = api.get("/api/v1/admin/accounts/" + wallet + "/entries", admin);
        assertThat(entries.body().get("totalItems").asLong()).isEqualTo(1);
        assertThat(api.get("/api/v1/admin/accounts/SYS-FUND-INR/entries", admin).body().get("totalItems").asLong()).isPositive();
        assertThat(api.get("/api/v1/admin/accounts/W-0000000000/entries", admin).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/admin/transactions/T-000000000000", admin).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("FR-ADM-07: statistics show counts, totals by type and currency, and the latest reconciliation")
    void statistics() {
        String admin = adminToken();
        String wallet = createWallet(registerUser("Stats").token(), "INR");
        LedgerSteps.fundOk(api, admin, wallet, 3_000);
        LedgerSteps.reconcile(api, admin);
        Response r = api.get("/api/v1/admin/statistics", admin);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body().get("users").asLong()).isPositive();
        assertThat(r.body().get("walletsByStatus").get("ACTIVE").asLong()).isPositive();
        assertThat(r.body().get("transactionsByStatus").has("FAILED")).isTrue();
        assertThat(r.body().get("completedTotals").findValuesAsText("type")).contains("TEST_FUNDING");
        assertThat(r.body().get("latestReconciliation").get("result").asText()).isEqualTo("PASS");
        long expectedFunding = count("SELECT SUM(amount_minor) FROM transaction_record WHERE type = 'TEST_FUNDING' "
                + "AND status = 'COMPLETED' AND currency = 'INR'");
        for (JsonNode t : r.body().get("completedTotals")) {
            if ("TEST_FUNDING".equals(t.get("type").asText()) && "INR".equals(t.get("currency").asText())) {
                assertThat(t.get("totalMinor").asLong()).isEqualTo(expectedFunding);
            }
        }
    }
}
