package com.quasar.ledger.module.idempotency.service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** The HTTP response to send: either freshly produced or replayed from storage (FR-TRF-05). */
public record IdempotentResponse(int status, String body, boolean replayed) {

    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    public ResponseEntity<String> toResponseEntity() {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON);
        if (replayed) {
            builder.header(REPLAYED_HEADER, "true");
        }
        return builder.body(body);
    }
}
