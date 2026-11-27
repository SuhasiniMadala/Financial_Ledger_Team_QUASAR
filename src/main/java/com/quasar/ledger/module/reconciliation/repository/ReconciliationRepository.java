package com.quasar.ledger.module.reconciliation.repository;

import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.module.reconciliation.dto.ReconciliationReport.Counts;
import com.quasar.ledger.module.reconciliation.dto.ReconciliationReport.CurrencyTotals;
import com.quasar.ledger.module.reconciliation.dto.ReconciliationReport.Discrepancy;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The read-only SQL behind checks C1-C3 (SRS 4.7.1). Every method only reads. They must all be called inside
 * one read transaction so they see the same committed snapshot (FR-REC-05).
 */
@Repository
public class ReconciliationRepository {

    private static final String DEBITS = "COALESCE(SUM(CASE WHEN e.direction = 'DEBIT' THEN e.amount_minor END), 0)";
    private static final String CREDITS = "COALESCE(SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount_minor END), 0)";

    private final JdbcTemplate jdbc;

    public ReconciliationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** C1 input: total debits and credits per currency. */
    public List<CurrencyTotals> currencyTotals() {
        return jdbc.query("SELECT a.currency, " + DEBITS + ", " + CREDITS + " FROM ledger_entry e JOIN account a ON a.id = e.account_id "
                        + "GROUP BY a.currency ORDER BY a.currency",
                (rs, i) -> new CurrencyTotals(rs.getString(1), rs.getLong(2), rs.getLong(3),
                        MoneyFormatter.format(rs.getLong(2), rs.getString(1)), MoneyFormatter.format(rs.getLong(3), rs.getString(1))));
    }

    /** C2: COMPLETED transactions with fewer than two entries or debits != credits. */
    public List<Discrepancy> unbalancedTransactions() {
        return jdbc.query("SELECT t.id, COUNT(e.id), " + DEBITS + ", " + CREDITS + " FROM transaction_record t "
                        + "LEFT JOIN ledger_entry e ON e.transaction_id = t.id WHERE t.status = 'COMPLETED' GROUP BY t.id "
                        + "HAVING COUNT(e.id) < 2 OR " + DEBITS + " <> " + CREDITS + " ORDER BY t.id",
                (rs, i) -> new Discrepancy("TRANSACTION_IMBALANCE", rs.getString(1), rs.getLong(3), rs.getLong(4),
                        rs.getLong(4) - rs.getLong(3), rs.getLong(2) + " entries; expected debits = credits and at least 2 entries"));
    }

    /** C2 (FR-LED-03): FAILED transactions must have no entries. */
    public List<Discrepancy> failedTransactionsWithEntries() {
        return jdbc.query("SELECT t.id, COUNT(e.id) FROM transaction_record t JOIN ledger_entry e ON e.transaction_id = t.id "
                        + "WHERE t.status = 'FAILED' GROUP BY t.id ORDER BY t.id",
                (rs, i) -> new Discrepancy("FAILED_TRANSACTION_HAS_ENTRIES", rs.getString(1), 0, rs.getLong(2), rs.getLong(2),
                        "FAILED transactions must have zero entries"));
    }

    /** C3 (ACC-02): stored balance vs ledger-derived balance (wallets: credits - debits; System Funding: debits - credits). */
    public List<Discrepancy> balanceMismatches() {
        return jdbc.query("SELECT a.id, a.balance_minor, CASE WHEN a.account_type = 'SYSTEM_FUNDING' THEN " + DEBITS + " - " + CREDITS
                        + " ELSE " + CREDITS + " - " + DEBITS + " END AS derived FROM account a "
                        + "LEFT JOIN ledger_entry e ON e.account_id = a.id GROUP BY a.id HAVING a.balance_minor <> derived ORDER BY a.id",
                (rs, i) -> new Discrepancy("BALANCE_MISMATCH", rs.getString(1), rs.getLong(3), rs.getLong(2),
                        rs.getLong(2) - rs.getLong(3), "stored balance differs from ledger-derived balance"));
    }

    public Counts counts() {
        return jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM account), (SELECT COUNT(*) FROM transaction_record), "
                + "(SELECT COUNT(*) FROM ledger_entry)", (rs, i) -> new Counts(rs.getLong(1), rs.getLong(2), rs.getLong(3)));
    }
}
