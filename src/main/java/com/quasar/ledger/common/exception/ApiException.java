package com.quasar.ledger.common.exception;

import com.quasar.ledger.common.api.ErrorCode;

/** A request failure that maps directly to an SRS error code and HTTP status. */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static ApiException notFound(String what) {
        return new ApiException(ErrorCode.NOT_FOUND, what + " not found");
    }
}
