package com.quasar.ledger.module.ledger.model;

/** BR-09: COMPLETED (balanced entries) or FAILED (rejected, no entries). Fixed at creation (FR-LED-06). */
public enum TransactionStatus {
    COMPLETED,
    FAILED
}
