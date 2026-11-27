package com.quasar.ledger.module.idempotency.service;

import com.quasar.ledger.common.api.ErrorCode;

/**
 * The result of a money operation that is stored for replay (FR-TRF-08: only 201 and 422 are stored).
 * Any other failure is thrown as an exception instead, which rolls everything back and stores nothing.
 */
public record FinancialOutcome(int status, Object body, ErrorCode errorCode, String message, String transactionId) {

    public static FinancialOutcome completed(Object body, String transactionId) {
        return new FinancialOutcome(201, body, null, null, transactionId);
    }

    /** A business-rule rejection (HTTP 422). {@code transactionId} is the FAILED record, if one was written. */
    public static FinancialOutcome rejected(ErrorCode code, String message, String transactionId) {
        if (code.status().value() != 422) {
            throw new IllegalArgumentException("Only 422 outcomes are stored; use an exception for " + code);
        }
        return new FinancialOutcome(422, null, code, message, transactionId);
    }
}
