package com.quasar.ledger.module.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** NFR-REL-02 / SK-02 step 4: UP only if a real database read succeeds. Read-only, public. */
@RestController
@Tag(name = "Health (public)")
public class HealthController {

    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);
    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/health")
    @Operation(summary = "Health check: reads the account table; 200 UP or 503 DOWN")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbc.queryForObject("SELECT COUNT(*) FROM account", Long.class);
            return ResponseEntity.ok(Map.of("status", "UP", "database", "UP"));
        } catch (RuntimeException e) {
            LOG.warn("Health check database read failed: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN", "database", "DOWN"));
        }
    }
}
