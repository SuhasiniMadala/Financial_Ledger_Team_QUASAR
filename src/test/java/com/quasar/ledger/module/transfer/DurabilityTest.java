package com.quasar.ledger.module.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.quasar.ledger.support.ApiClient;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.AppProcess;
import com.quasar.ledger.support.LedgerSteps;
import com.quasar.ledger.support.TransferSteps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * NFR-SAFE-04: "A transaction acknowledged with 201 survives a forced process kill. Test: kill immediately after 201,
 * restart, verify data and reconciliation PASS." Runs the real application as a separate process.
 */
@Tag("system")
class DurabilityTest {

    @Test
    @DisplayName("Kill the server right after a 201, restart on the same database file: the transfer is still there; PASS")
    void acknowledgedTransactionSurvivesKill() throws Exception {
        Path dir = Files.createDirectories(Path.of("target", "durability"));
        Path db = dir.resolve("durability-" + UUID.randomUUID() + ".db");
        String a;
        String b;
        String transferId;
        try (AppProcess first = AppProcess.start(db, dir.resolve("first-run.log"))) {
            ApiClient api = new ApiClient(first.baseUrl(), JsonMapper.builder().build());
            String admin = login(api, AppProcess.ADMIN_EMAIL, AppProcess.ADMIN_PASSWORD);
            String asha = register(api, "asha");
            String ravi = register(api, "ravi");
            a = api.post("/api/v1/wallets", Map.of("currency", "INR"), asha).text("walletId");
            b = api.post("/api/v1/wallets", Map.of("currency", "INR"), ravi).text("walletId");
            LedgerSteps.fundOk(api, admin, a, 500_000);
            Response ack = TransferSteps.transfer(api, asha, a, b, 123_456, "durable-" + UUID.randomUUID());
            assertThat(ack.status()).isEqualTo(201);
            transferId = ack.text("transactionId");
            first.kill(); // immediately after the 201
        }

        try (AppProcess second = AppProcess.start(db, dir.resolve("second-run.log"))) {
            ApiClient api = new ApiClient(second.baseUrl(), JsonMapper.builder().build());
            String admin = login(api, AppProcess.ADMIN_EMAIL, AppProcess.ADMIN_PASSWORD);
            Response detail = api.get("/api/v1/admin/transactions/" + transferId, admin);
            assertThat(detail.status()).isEqualTo(200);
            assertThat(detail.body().get("transaction").get("status").asText()).isEqualTo("COMPLETED");
            assertThat(detail.body().get("entries")).hasSize(2);
            assertThat(api.get("/api/v1/admin/accounts/" + a + "/entries", admin).body().get("items").get(0)
                    .get("balanceAfterMinor").asLong()).isEqualTo(500_000 - 123_456);
            assertThat(api.get("/api/v1/admin/accounts/" + b + "/entries", admin).body().get("items").get(0)
                    .get("balanceAfterMinor").asLong()).isEqualTo(123_456);
            LedgerSteps.assertReconciles(api, admin);
        }
    }

    private static String login(ApiClient api, String email, String password) {
        Response r = api.post("/api/v1/auth/login", Map.of("email", email, "password", password), null);
        assertThat(r.status()).as("%s", r).isEqualTo(200);
        return r.text("accessToken");
    }

    private static String register(ApiClient api, String name) {
        String email = name + "-" + UUID.randomUUID() + "@durability.test";
        assertThat(api.post("/api/v1/auth/register", Map.of("displayName", name, "email", email, "password", "Password-123"), null)
                .status()).isEqualTo(201);
        return login(api, email, "Password-123");
    }
}
