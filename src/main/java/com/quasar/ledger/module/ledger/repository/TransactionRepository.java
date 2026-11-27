package com.quasar.ledger.module.ledger.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.util.DateRange;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.model.TypeCurrencyTotal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistence port for transactions. Insert-only: there is no update or delete (FR-LED-05). */
public interface TransactionRepository {

    void insert(TransactionRecord record);

    Optional<TransactionRecord> findById(String id);

    Optional<TransactionRecord> findReversalOf(String originalId);

    /** Map original transaction ID -> ID of the REVERSAL that reverses it. */
    Map<String, String> findReversalIds(Collection<String> originalIds);

    List<TransactionRecord> search(TransactionSearch search, PageQuery page);

    long count(TransactionSearch search);

    Map<TransactionStatus, Long> countByStatus();

    List<TypeCurrencyTotal> completedTotals();

    /** Count and total of COMPLETED MERCHANT_PAYMENTs credited to a wallet in a date range (FR-PAY-07). */
    long[] merchantPaymentCountAndTotal(String merchantWalletId, DateRange range);
}
