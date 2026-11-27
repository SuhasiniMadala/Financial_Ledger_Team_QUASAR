package com.quasar.ledger.module.reconciliation.dto;

import java.util.List;

/**
 * FR-REC-07: run time, overall result, per-currency totals, counts checked, and each discrepancy with type,
 * subject, expected, actual and difference (actual - expected). FR-REC-08: PASS if and only if no discrepancy.
 */
public record ReconciliationReport(String runId, String runAt, long durationMs, String result, List<CurrencyTotals> currencies,
                                   Counts checked, List<Discrepancy> discrepancies) {

    public record CurrencyTotals(String currency, long totalDebitsMinor, long totalCreditsMinor, String totalDebitsFormatted,
                                 String totalCreditsFormatted) {
    }

    public record Counts(long accounts, long transactions, long entries) {
    }

    /**
     * @param type CURRENCY_IMBALANCE (C1), TRANSACTION_IMBALANCE or FAILED_TRANSACTION_HAS_ENTRIES (C2), BALANCE_MISMATCH (C3)
     */
    public record Discrepancy(String type, String subjectId, long expectedMinor, long actualMinor, long differenceMinor,
                              String detail) {
    }
}
