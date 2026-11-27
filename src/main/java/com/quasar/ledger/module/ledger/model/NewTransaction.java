package com.quasar.ledger.module.ledger.model;

/** The descriptive part of a transaction to record; the Ledger assigns ID, status and time. */
public record NewTransaction(TransactionType type, long amountMinor, String currency, String initiatedBy,
                             String sourceAccountId, String destinationAccountId, String reversalOfId, String description) {
}
