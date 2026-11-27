package com.quasar.ledger.module.admin.service;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.api.PageResponse;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.module.admin.dto.TransactionDetailResponse;
import com.quasar.ledger.module.ledger.dto.LedgerEntryView;
import com.quasar.ledger.module.ledger.dto.TransactionView;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.repository.LedgerEntryRepository;
import com.quasar.ledger.module.ledger.repository.TransactionRepository;
import com.quasar.ledger.module.ledger.repository.TransactionSearch;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-ADM-03: admins list all transactions (filters: type, status, date, wallet) and view entries of any transaction or account. */
@Service
public class AdminLedgerQueryService {

    private final TransactionRepository transactions;
    private final LedgerEntryRepository entries;
    private final AccountRepository accounts;

    public AdminLedgerQueryService(TransactionRepository transactions, LedgerEntryRepository entries, AccountRepository accounts) {
        this.transactions = transactions;
        this.entries = entries;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionView> list(TransactionSearch search, PageQuery page) {
        List<TransactionRecord> rows = transactions.search(search, page);
        Map<String, String> reversedBy = transactions.findReversalIds(rows.stream().map(TransactionRecord::id).toList());
        List<TransactionView> items = rows.stream().map(t -> TransactionView.of(t, reversedBy.get(t.id()))).toList();
        return PageResponse.of(items, page, transactions.count(search));
    }

    @Transactional(readOnly = true)
    public TransactionDetailResponse detail(String transactionId) {
        TransactionRecord t = transactions.findById(transactionId).orElseThrow(() -> ApiException.notFound("Transaction"));
        String reversedBy = transactions.findReversalOf(t.id()).map(TransactionRecord::id).orElse(null);
        return new TransactionDetailResponse(TransactionView.of(t, reversedBy),
                entries.findByTransaction(t.id()).stream().map(LedgerEntryView::of).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryView> accountEntries(String accountId, PageQuery page) {
        accounts.findById(accountId).orElseThrow(() -> ApiException.notFound("Account"));
        List<LedgerEntryView> items = entries.findByAccount(accountId, page).stream().map(LedgerEntryView::of).toList();
        return PageResponse.of(items, page, entries.countByAccount(accountId));
    }
}
