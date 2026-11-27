package com.quasar.ledger.module.reconciliation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quasar.ledger.common.id.IdGenerator;
import com.quasar.ledger.common.id.IdKind;
import com.quasar.ledger.common.util.Timestamps;
import com.quasar.ledger.module.reconciliation.dto.ReconciliationReport;
import com.quasar.ledger.module.reconciliation.dto.ReconciliationReport.CurrencyTotals;
import com.quasar.ledger.module.reconciliation.dto.ReconciliationReport.Discrepancy;
import com.quasar.ledger.module.reconciliation.repository.ReconciliationRepository;
import com.quasar.ledger.module.reconciliation.repository.ReconciliationRunRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-REC-01..08 / ADR-007: a read-only audit of the ledger. All checks run inside ONE read-only database
 * transaction, which in WAL mode reads a single committed snapshot without blocking writers (FR-REC-05).
 * It reports discrepancies but never repairs them (FR-REC-06).
 */
@Service
public class ReconciliationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationService.class);

    private final ReconciliationRepository checks;
    private final ReconciliationRunRepository runs;
    private final TransactionTemplate snapshot;
    private final TransactionTemplate writeTransaction;
    private final IdGenerator ids;
    private final ObjectMapper json;
    private final Clock clock;

    public ReconciliationService(ReconciliationRepository checks, ReconciliationRunRepository runs, PlatformTransactionManager tm,
                                 TransactionTemplate writeTransaction, IdGenerator ids, ObjectMapper json, Clock clock) {
        this.checks = checks;
        this.runs = runs;
        this.snapshot = new TransactionTemplate(tm);
        this.snapshot.setReadOnly(true);
        this.writeTransaction = writeTransaction;
        this.ids = ids;
        this.json = json;
        this.clock = clock;
    }

    public ReconciliationReport run() {
        Instant start = clock.instant();
        ReconciliationReport report = snapshot.execute(status -> {
            List<CurrencyTotals> totals = checks.currencyTotals();
            List<Discrepancy> found = new ArrayList<>();
            for (CurrencyTotals t : totals) {
                if (t.totalDebitsMinor() != t.totalCreditsMinor()) {
                    found.add(new Discrepancy("CURRENCY_IMBALANCE", t.currency(), t.totalDebitsMinor(), t.totalCreditsMinor(),
                            t.totalCreditsMinor() - t.totalDebitsMinor(), "total debits must equal total credits"));
                }
            }
            found.addAll(checks.unbalancedTransactions());
            found.addAll(checks.failedTransactionsWithEntries());
            found.addAll(checks.balanceMismatches());
            long ms = Duration.between(start, clock.instant()).toMillis();
            return new ReconciliationReport(ids.generate(IdKind.RECONCILIATION_RUN), Timestamps.format(start), ms,
                    found.isEmpty() ? "PASS" : "FAIL", totals, checks.counts(), found);
        });
        record(report);
        LOG.info("Reconciliation {} finished: {} with {} discrepancies in {} ms", report.runId(), report.result(),
                report.discrepancies().size(), report.durationMs());
        return report;
    }

    /** Stores the run summary after the snapshot is closed, in its own small write transaction. */
    private void record(ReconciliationReport report) {
        try {
            String body = json.writeValueAsString(report);
            writeTransaction.executeWithoutResult(s -> runs.insert(report.runId(), report.runAt(), report.result(),
                    report.discrepancies().size(), body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise reconciliation report", e);
        }
    }
}
