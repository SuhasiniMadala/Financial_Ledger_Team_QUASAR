package com.quasar.ledger.module.admin.dto;

import java.util.List;
import java.util.Map;

/** FR-ADM-07 platform statistics. */
public record StatisticsResponse(long users, long merchants, Map<String, Long> walletsByStatus, Map<String, Long> transactionsByStatus,
                                 List<CompletedTotal> completedTotals, LatestReconciliation latestReconciliation) {

    public record CompletedTotal(String type, String currency, long count, long totalMinor, String totalFormatted) {
    }

    public record LatestReconciliation(String runId, String runAt, String result, int discrepancyCount) {
    }
}
