package com.quasar.ledger.module.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** NFR-REL-02: GET /health reports UP only when a database read succeeds. */
class HealthApiTest extends ApiTestBase {

    @Test
    @DisplayName("GET /api/v1/health is public and returns UP")
    void healthIsUp() {
        Response r = api.get("/api/v1/health", null);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.text("status")).isEqualTo("UP");
        assertThat(r.text("database")).isEqualTo("UP");
    }

    @Test
    @DisplayName("Reports DOWN with 503 when the database read fails")
    void healthIsDownWhenDatabaseFails() {
        JdbcTemplate broken = mock(JdbcTemplate.class);
        when(broken.queryForObject(anyString(), any(Class.class))).thenThrow(new DataAccessResourceFailureException("disk gone"));
        ResponseEntity<Map<String, String>> result = new HealthController(broken).health();
        assertThat(result.getStatusCode().value()).isEqualTo(503);
        assertThat(result.getBody()).containsEntry("status", "DOWN");
    }
}
