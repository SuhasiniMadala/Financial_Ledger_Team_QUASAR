package com.quasar.ledger.common.exception;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.api.ErrorResponse;
import com.quasar.ledger.common.web.ErrorResponseFactory;
import com.quasar.ledger.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps every failure to the SRS error format (NFR-REL-01). Messages never contain stack traces,
 * SQL or class names (NFR-SEC-06); unexpected errors are logged server-side with the requestId.
 * Any exception thrown inside a database transaction has already caused a full rollback.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final int SQLITE_BUSY = 5;
    private static final int SQLITE_LOCKED = 6;

    private final ErrorResponseFactory errors;

    public GlobalExceptionHandler(ErrorResponseFactory errors) {
        this.errors = errors;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        return respond(ex.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return respond(ErrorCode.VALIDATION_ERROR, details.isEmpty() ? "Request is invalid" : details, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleUnreadable(Exception ex, HttpServletRequest request) {
        // Covers malformed JSON, unknown fields, fractional or string amounts (TQ-6, NFR-SEC-04).
        return respond(ErrorCode.VALIDATION_ERROR,
                "Malformed request body: JSON must be valid, use only documented fields, and amounts must be integers",
                request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
        ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleBadParameter(Exception ex, HttpServletRequest request) {
        return respond(ErrorCode.VALIDATION_ERROR, "A request parameter is missing or malformed", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoRoute(NoResourceFoundException ex, HttpServletRequest request) {
        return respond(ErrorCode.NOT_FOUND, "No such endpoint", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ErrorResponse body = errors.create(ErrorCode.VALIDATION_ERROR, "HTTP method not supported for this endpoint", request);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (isDatabaseBusy(ex)) {
            // FR-CON-06: write lock not obtained within the busy timeout. Nothing was changed; retry with the same key.
            ErrorResponse body = errors.create(ErrorCode.SERVICE_BUSY,
                    "The ledger is busy; nothing was changed. Retry with the same Idempotency-Key.", request);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, "1").body(body);
        }
        LOG.error("Unhandled error for requestId={} path={}", RequestIdFilter.requestId(request), request.getRequestURI(), ex);
        return respond(ErrorCode.INTERNAL_ERROR, "Internal error. The operation was rolled back and had no effect.", request);
    }

    private ResponseEntity<ErrorResponse> respond(ErrorCode code, String message, HttpServletRequest request) {
        return ResponseEntity.status(code.status()).body(errors.create(code, message, request));
    }

    /** True if any cause in the chain is SQLite's SQLITE_BUSY / SQLITE_LOCKED result. */
    static boolean isDatabaseBusy(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                int primary = sql.getErrorCode() & 0xFF;
                if (primary == SQLITE_BUSY || primary == SQLITE_LOCKED) {
                    return true;
                }
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
