package com.quasar.ledger.module.ledger.dto;

import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.module.ledger.model.TransactionRecord;

/** Body of a 201 for funding, transfers, payments and reversals (FR-TRF-14: includes the transaction ID). */
public record TransactionReceipt(String transactionId, String type, String status, long amountMinor, String amountFormatted,
                                 String currency, String sourceAccountId, String destinationAccountId, String reversalOfId,
                                 String description, String createdAt) {

    public static TransactionReceipt of(TransactionRecord t) {
        return new TransactionReceipt(t.id(), t.type().name(), t.status().name(), t.amountMinor(),
                MoneyFormatter.format(t.amountMinor(), t.currency()), t.currency(), t.sourceAccountId(), t.destinationAccountId(),
                t.reversalOfId(), t.description(), t.createdAt());
    }
}
