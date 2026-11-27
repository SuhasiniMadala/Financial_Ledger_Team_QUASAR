package com.quasar.ledger.common.web;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.api.ErrorResponse;
import com.quasar.ledger.common.util.Timestamps;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** Builds the standard error body (code, message, timestamp, path, requestId). */
@Component
public class ErrorResponseFactory {

    private final Clock clock;

    public ErrorResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public ErrorResponse create(ErrorCode code, String message, HttpServletRequest request) {
        return create(code, message, request.getRequestURI(), RequestIdFilter.requestId(request), null);
    }

    public ErrorResponse create(ErrorCode code, String message, String path, String requestId, String transactionId) {
        return new ErrorResponse(code.name(), message, Timestamps.format(clock.instant()), path, requestId, transactionId);
    }
}
