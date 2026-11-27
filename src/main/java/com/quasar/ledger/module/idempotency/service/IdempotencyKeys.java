package com.quasar.ledger.module.idempotency.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import java.util.regex.Pattern;

/** FR-TRF-02 / FR-TRF-03: the Idempotency-Key header is required and must be 8-64 of [A-Za-z0-9_-]. */
public final class IdempotencyKeys {

    public static final String HEADER = "Idempotency-Key";
    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    private IdempotencyKeys() {
    }

    public static String validate(String key) {
        if (key == null || key.isEmpty()) {
            throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED, "The Idempotency-Key header is required for this operation");
        }
        if (!FORMAT.matcher(key).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "Idempotency-Key must be 8-64 characters of letters, digits, '-' and '_'");
        }
        return key;
    }
}
