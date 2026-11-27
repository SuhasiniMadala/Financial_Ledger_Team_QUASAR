package com.quasar.ledger.module.reconciliation.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Log of reconciliation runs. This is operational metadata, not financial data (FR-REC-06 is unaffected). */
@Repository
public class ReconciliationRunRepository {

    public record RunSummary(String runId, String runAt, String result, int discrepancyCount) {
    }

    private final JdbcTemplate jdbc;

    public ReconciliationRunRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String runId, String runAt, String result, int discrepancyCount, String reportJson) {
        jdbc.update("INSERT INTO reconciliation_run (id, run_at, result, discrepancy_count, report_json) VALUES (?, ?, ?, ?, ?)",
                runId, runAt, result, discrepancyCount, reportJson);
    }

    public Optional<RunSummary> latest() {
        return jdbc.query("SELECT id, run_at, result, discrepancy_count FROM reconciliation_run ORDER BY run_at DESC, rowid DESC LIMIT 1",
                (rs, i) -> new RunSummary(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4))).stream().findFirst();
    }
}
