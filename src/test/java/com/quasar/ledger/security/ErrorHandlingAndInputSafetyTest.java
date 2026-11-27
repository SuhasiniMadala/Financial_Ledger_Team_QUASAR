package com.quasar.ledger.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** NFR-REL-01 (error format), NFR-SEC-04 (validation), NFR-SEC-05 (SQL injection), NFR-SEC-06 (no internals leaked). */
class ErrorHandlingAndInputSafetyTest extends ApiTestBase {

    @Test
    @DisplayName("Every error has code, message, timestamp, path and requestId; requestId matches X-Request-Id")
    void errorBodyHasTheStandardShape() {
        Response r = api.postRaw("/api/v1/auth/register", "{not json", null);
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(r.text("message")).isNotBlank();
        assertThat(r.text("timestamp")).endsWith("Z");
        assertThat(r.text("path")).isEqualTo("/api/v1/auth/register");
        assertThat(r.text("requestId")).isNotBlank().isEqualTo(r.header("X-Request-Id"));
    }

    @Test
    @DisplayName("NFR-SEC-06: responses never contain stack traces, SQL or class names")
    void noInternalsLeak() {
        String[] bad = {"{not json", "{\"displayName\": 5, \"email\": [], \"password\": {}}", "[]", "\"\""};
        for (String body : bad) {
            Response r = api.postRaw("/api/v1/auth/register", body, null);
            assertThat(r.status()).isEqualTo(400);
            assertThat(r.raw().body()).doesNotContain("Exception").doesNotContain("java.").doesNotContain("com.quasar")
                    .doesNotContain("SQL").doesNotContain("at org.");
        }
        Response unknownRoute = api.get("/api/v1/health/../../etc/passwd", null);
        assertThat(unknownRoute.raw().body()).doesNotContain("Exception");
    }

    @Test
    void unknownEndpointIs404InStandardFormat() {
        String token = registerUser("Route User").token();
        Response r = api.get("/api/v1/no-such-endpoint", token);
        assertThat(r.status()).isEqualTo(404);
        assertThat(r.code()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("NFR-SEC-05: SQL-injection strings in text fields are stored as plain text, never executed")
    void sqlInjectionIsNotExecuted() {
        String evilName = "Robert'); DROP TABLE account; --";
        String email = uniqueEmail("bobby");
        Response r = api.post("/api/v1/auth/register", Map.of("displayName", evilName, "email", email, "password", PASSWORD), null);
        assertThat(r.status()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT display_name FROM user_account WHERE id = ?", String.class, r.text("accountId")))
                .isEqualTo(evilName);
        assertThat(count("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'account'")).isEqualTo(1);

        Response login = api.post("/api/v1/auth/login", Map.of("email", "' OR '1'='1", "password", "' OR '1'='1"), null);
        assertThat(login.status()).isEqualTo(401);
        Response wallet = api.get("/api/v1/wallets/W-' OR 1=1 --".replace(" ", "%20"), registerUser("Inj").token());
        assertThat(wallet.status()).isEqualTo(400);
    }
}
