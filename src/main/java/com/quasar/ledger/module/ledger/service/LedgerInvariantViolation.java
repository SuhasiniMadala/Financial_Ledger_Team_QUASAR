package com.quasar.ledger.module.ledger.service;

/**
 * Thrown when a posting would break a ledger invariant (unbalanced, under two entries, negative balance,
 * currency mismatch). It signals a programming error, so it surfaces as HTTP 500 and the whole database
 * transaction is rolled back (FR-LED-02, NFR-REL-01).
 */
public class LedgerInvariantViolation extends RuntimeException {

    public LedgerInvariantViolation(String message) {
        super(message);
    }
}
