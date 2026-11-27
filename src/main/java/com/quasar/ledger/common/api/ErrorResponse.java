package com.quasar.ledger.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The single error body used by every endpoint (SRS 3.4, NFR-REL-01).
 * {@code transactionId} is only present when a FAILED transaction was recorded (FR-TRF-13).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String timestamp,
        String path,
        String requestId,
        String transactionId) {

    public static ErrorResponse of(ErrorCode code, String message, String timestamp, String path, String requestId) {
        return new ErrorResponse(code.name(), message, timestamp, path, requestId, null);
    }
}
