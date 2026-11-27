package com.quasar.ledger.module.ledger.dto;

import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.module.ledger.model.TransactionRecord;

/** Full transaction row for administrators (FR-ADM-03). "reversedBy" links to the REVERSAL, if any (BR-09). */
public record TransactionView(String transactionId, String type, String status, long amountMinor, String amountFormatted,
                              String currency, String initiatedBy, String sourceAccountId, String destinationAccountId,
                              String reversalOfId, String reversedBy, String failureCode, String description, String createdAt) {

    public static TransactionView of(TransactionRecord t, String reversedBy) {
        return new TransactionView(t.id(), t.type().name(), t.status().name(), t.amountMinor(),
                MoneyFormatter.format(t.amountMinor(), t.currency()), t.currency(), t.initiatedBy(), t.sourceAccountId(),
                t.destinationAccountId(), t.reversalOfId(), reversedBy, t.failureCode(), t.description(), t.createdAt());
    }
}
