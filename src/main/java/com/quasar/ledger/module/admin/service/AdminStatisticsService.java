package com.quasar.ledger.module.admin.service;

import com.quasar.ledger.common.security.Role;
import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.module.admin.dto.StatisticsResponse;
import com.quasar.ledger.module.auth.repository.UserAccountRepository;
import com.quasar.ledger.module.ledger.repository.TransactionRepository;
import com.quasar.ledger.module.reconciliation.repository.ReconciliationRunRepository;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-ADM-07: users, merchants, wallets by status, COMPLETED/FAILED counts, COMPLETED totals by type and currency,
 * and the latest reconciliation result.
 */
@Service
public class AdminStatisticsService {

    private final UserAccountRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final ReconciliationRunRepository runs;

    public AdminStatisticsService(UserAccountRepository users, AccountRepository accounts, TransactionRepository transactions,
                                  ReconciliationRunRepository runs) {
        this.users = users;
        this.accounts = accounts;
        this.transactions = transactions;
        this.runs = runs;
    }

    @Transactional(readOnly = true)
    public StatisticsResponse statistics() {
        Map<String, Long> wallets = new LinkedHashMap<>();
        accounts.countWalletsByStatus().forEach((k, v) -> wallets.put(k.name(), v));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        transactions.countByStatus().forEach((k, v) -> byStatus.put(k.name(), v));
        var totals = transactions.completedTotals().stream()
                .map(t -> new StatisticsResponse.CompletedTotal(t.type().name(), t.currency(), t.count(), t.totalMinor(),
                        MoneyFormatter.format(t.totalMinor(), t.currency())))
                .toList();
        var latest = runs.latest()
                .map(r -> new StatisticsResponse.LatestReconciliation(r.runId(), r.runAt(), r.result(), r.discrepancyCount()))
                .orElse(null);
        return new StatisticsResponse(users.countByRole(Role.USER), users.countByRole(Role.MERCHANT), wallets, byStatus, totals, latest);
    }
}
