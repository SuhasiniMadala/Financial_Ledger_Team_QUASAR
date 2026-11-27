package com.quasar.ledger.module.ledger.model;

/** One immutable debit or credit line (FR-LED-04): ID, transaction, account, direction, amount, sequence, time, balance after. */
public record LedgerEntry(long sequenceNo, String id, String transactionId, String accountId, EntryDirection direction,
                          long amountMinor, long balanceAfterMinor, String createdAt) {
}
