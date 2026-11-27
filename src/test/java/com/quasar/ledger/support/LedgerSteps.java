package com.quasar.ledger.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient.Response;
import java.util.Map;
import java.util.UUID;

/** Reusable API steps for funding, reversal and reconciliation (owned by the Ledger & Reconciliation module). */
public final class LedgerSteps {

    private LedgerSteps() {
    }

    public static Response fund(ApiClient api, String adminToken, String walletId, long amountMinor, String key) {
        return api.postIdempotent("/api/v1/admin/wallets/" + walletId + "/test-funding", Map.of("amountMinor", amountMinor),
                adminToken, key);
    }

    /** Funds a wallet and asserts 201. Returns the transaction ID. */
    public static String fundOk(ApiClient api, String adminToken, String walletId, long amountMinor) {
        Response r = fund(api, adminToken, walletId, amountMinor, "fund-" + UUID.randomUUID());
        assertThat(r.status()).as("fund %s", r).isEqualTo(201);
        return r.text("transactionId");
    }

    public static Response reverse(ApiClient api, String adminToken, String transactionId, String reason, String key) {
        return api.postIdempotent("/api/v1/admin/transactions/" + transactionId + "/reversal", Map.of("reason", reason), adminToken, key);
    }

    public static Response reconcile(ApiClient api, String adminToken) {
        Response r = api.post("/api/v1/admin/reconciliations", null, adminToken);
        assertThat(r.status()).as("reconcile %s", r).isEqualTo(200);
        return r;
    }

    /** NFR-SAFE-02: after any mix of operations the ledger must reconcile with zero discrepancies. */
    public static void assertReconciles(ApiClient api, String adminToken) {
        Response r = reconcile(api, adminToken);
        assertThat(r.text("result")).as("reconciliation report %s", r).isEqualTo("PASS");
        assertThat(r.body().get("discrepancies")).isEmpty();
    }
}
