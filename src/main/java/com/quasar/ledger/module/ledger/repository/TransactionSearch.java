package com.quasar.ledger.module.ledger.repository;

import com.quasar.ledger.common.util.DateRange;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.model.TransactionType;
import java.util.Set;

/**
 * Filters for listing transactions. All fields are optional.
 *
 * @param accountId         only transactions whose source or destination is this account
 * @param visibleAccounts   if set, COMPLETED transactions must involve one of these accounts ...
 * @param failedVisibleTo   ... and FAILED transactions must have been initiated by this account (FR-LED-11)
 */
public record TransactionSearch(String accountId, TransactionType type, TransactionStatus status, DateRange range,
                                Set<String> visibleAccounts, String failedVisibleTo) {

    public static TransactionSearch admin(String accountId, TransactionType type, TransactionStatus status, DateRange range) {
        return new TransactionSearch(accountId, type, status, range, null, null);
    }
}
