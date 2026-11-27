package com.quasar.ledger.module.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import com.quasar.ledger.support.TransferSteps;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * SRS 4.5.2 #4 / FR-CON-06: if another writer holds the SQLite write lock longer than the busy timeout, the
 * request is aborted with no effect and HTTP 503 SERVICE_BUSY + Retry-After; a retry with the same key then works.
 * A short timeout (300 ms) keeps the test fast; it has its own Spring context and database.
 */
@TestPropertySource(properties = "ledger.db.busy-timeout-ms=300")
class BusyTimeoutTest extends ApiTestBase {

    /**
     * The file the running application really uses. Do NOT read ${ledger.db.path} here: it contains ${random.uuid},
     * which Spring re-generates on every lookup, so it would name a different file (regression for DEF-002).
     */
    private String applicationDatabaseFile() {
        String file = jdbc.queryForList("PRAGMA database_list").stream()
                .filter(row -> "main".equals(row.get("name")))
                .map(row -> String.valueOf(row.get("file")))
                .findFirst().orElseThrow();
        assertThat(Path.of(file)).exists();
        assertThat(file.replace('\\', '/')).contains("target/test-db/");
        return file;
    }

    @Test
    @DisplayName("Write lock held elsewhere -> 503 SERVICE_BUSY, nothing changed; retry with the same key -> 201 once")
    void busyWriteLockGives503() throws Exception {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        String a = createWallet(asha.token(), "INR");
        String b = createWallet(registerUser("Ravi Kumar").token(), "INR");
        LedgerSteps.fundOk(api, admin, a, 50_000);
        String key = newKey();

        try (Connection other = DriverManager.getConnection("jdbc:sqlite:" + applicationDatabaseFile());
             Statement st = other.createStatement()) {
            st.execute("BEGIN IMMEDIATE"); // another process/connection holds the single write lock
            Response busy = TransferSteps.transfer(api, asha.token(), a, b, 1_000, key);
            assertThat(busy.status()).as("%s", busy).isEqualTo(503);
            assertThat(busy.code()).isEqualTo("SERVICE_BUSY");
            assertThat(busy.header("Retry-After")).isEqualTo("1");
            // Reads are not blocked by the writer in WAL mode.
            assertThat(api.get("/api/v1/wallets/" + a, asha.token()).status()).isEqualTo(200);
            st.execute("ROLLBACK");
        }

        assertThat(storedBalance(a)).isEqualTo(50_000);
        assertThat(count("SELECT COUNT(*) FROM idempotency_record WHERE idem_key = ?", key)).isZero();
        Response retry = TransferSteps.transfer(api, asha.token(), a, b, 1_000, key);
        assertThat(retry.status()).isEqualTo(201);
        assertThat(retry.header("Idempotent-Replayed")).isNull();
        assertThat(storedBalance(a)).isEqualTo(49_000);
        LedgerSteps.assertReconciles(api, admin);
    }
}
