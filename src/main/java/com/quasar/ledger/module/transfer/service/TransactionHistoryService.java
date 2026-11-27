package com.quasar.ledger.module.transfer.service;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.api.PageResponse;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.util.DateRange;
import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.module.ledger.dto.LedgerEntryView;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.repository.LedgerEntryRepository;
import com.quasar.ledger.module.ledger.repository.TransactionRepository;
import com.quasar.ledger.module.ledger.repository.TransactionSearch;
import com.quasar.ledger.module.transfer.dto.UserTransactionDetail;
import com.quasar.ledger.module.transfer.dto.UserTransactionView;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-LED-11/12: a USER lists COMPLETED transactions on their own wallets (incoming and outgoing) and their own FAILED
 * attempts, newest first. Anything else is 404 (FR-AUTH-09). Only the caller's own entries are ever returned.
 */
@Service
public class TransactionHistoryService {

    private final TransactionRepository transactions;
    private final LedgerEntryRepository entries;
    private final AccountRepository accounts;

    public TransactionHistoryService(TransactionRepository transactions, LedgerEntryRepository entries, AccountRepository accounts) {
        this.transactions = transactions;
        this.entries = entries;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserTransactionView> list(String callerId, String walletId, TransactionType type, TransactionStatus status,
                                                  DateRange range, PageQuery page) {
        Set<String> mine = ownWalletIds(callerId);
        if (walletId != null && !mine.contains(walletId)) {
            throw ApiException.notFound("Wallet");
        }
        TransactionSearch search = new TransactionSearch(walletId, type, status, range, mine, callerId);
        List<TransactionRecord> rows = transactions.search(search, page);
        Map<String, String> reversedBy = transactions.findReversalIds(rows.stream().map(TransactionRecord::id).toList());
        Map<String, String> names = accounts.displayNames(counterparties(rows, mine));
        List<UserTransactionView> items = rows.stream().map(t -> view(t, mine, names, reversedBy.get(t.id()))).toList();
        return PageResponse.of(items, page, transactions.count(search));
    }

    @Transactional(readOnly = true)
    public UserTransactionDetail detail(String callerId, String transactionId) {
        Set<String> mine = ownWalletIds(callerId);
        TransactionRecord t = transactions.findById(transactionId)
                .filter(r -> visible(r, mine, callerId))
                .orElseThrow(() -> ApiException.notFound("Transaction"));
        String reversedBy = transactions.findReversalOf(t.id()).map(TransactionRecord::id).orElse(null);
        Map<String, String> names = accounts.displayNames(counterparties(List.of(t), mine));
        List<LedgerEntryView> own = entries.findByTransaction(t.id()).stream()
                .filter(e -> mine.contains(e.accountId()))
                .map(LedgerEntryView::of)
                .toList();
        return new UserTransactionDetail(view(t, mine, names, reversedBy), own);
    }

    private Set<String> ownWalletIds(String callerId) {
        return accounts.findByOwner(callerId).stream().map(Account::id).collect(Collectors.toSet());
    }

    private static boolean visible(TransactionRecord t, Set<String> mine, String callerId) {
        if (t.status() == TransactionStatus.FAILED) {
            return callerId.equals(t.initiatedBy());
        }
        return mine.contains(t.sourceAccountId()) || mine.contains(t.destinationAccountId());
    }

    private static Set<String> counterparties(List<TransactionRecord> rows, Set<String> mine) {
        Set<String> ids = new HashSet<>();
        for (TransactionRecord t : rows) {
            ids.add(counterparty(t, mine));
        }
        ids.remove(null);
        return ids;
    }

    private static String counterparty(TransactionRecord t, Set<String> mine) {
        return mine.contains(t.sourceAccountId()) ? t.destinationAccountId() : t.sourceAccountId();
    }

    private static UserTransactionView view(TransactionRecord t, Set<String> mine, Map<String, String> names, String reversedBy) {
        boolean outgoing = mine.contains(t.sourceAccountId());
        String myWallet = outgoing ? t.sourceAccountId() : t.destinationAccountId();
        String other = counterparty(t, mine);
        return new UserTransactionView(t.id(), t.type().name(), t.status().name(), outgoing ? "OUTGOING" : "INCOMING", myWallet,
                t.amountMinor(), MoneyFormatter.format(t.amountMinor(), t.currency()), t.currency(),
                other == null ? null : names.get(other), t.description(), t.failureCode(),
                t.reversalOfId(), reversedBy, t.createdAt());
    }
}
