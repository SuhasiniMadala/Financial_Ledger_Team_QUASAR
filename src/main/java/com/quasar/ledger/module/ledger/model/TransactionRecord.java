package com.quasar.ledger.module.ledger.model;

/** An immutable financial event (FR-LED-05/06). source/destination are set for every type, including FAILED attempts. */
public record TransactionRecord(String id, TransactionType type, TransactionStatus status, long amountMinor, String currency,
                                String initiatedBy, String sourceAccountId, String destinationAccountId, String reversalOfId,
                                String failureCode, String description, String createdAt) {
}
