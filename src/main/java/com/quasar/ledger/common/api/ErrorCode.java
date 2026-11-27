package com.quasar.ledger.common.api;

import org.springframework.http.HttpStatus;

/** Every error code of SRS Table 3-2, with its HTTP status. */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT),
    ALREADY_REVERSED(HttpStatus.CONFLICT),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY),
    WALLET_NOT_ACTIVE(HttpStatus.UNPROCESSABLE_ENTITY),
    CURRENCY_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY),
    SAME_WALLET(HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_DESTINATION(HttpStatus.UNPROCESSABLE_ENTITY),
    NOT_REVERSIBLE(HttpStatus.UNPROCESSABLE_ENTITY),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_BUSY(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
