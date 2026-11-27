package com.quasar.ledger.module.idempotency.model;

/** A stored outcome for (caller, key): fingerprint, HTTP status (201 or 422), body and linked transaction (FR-TRF-04/08). */
public record IdempotencyRecord(String principalId, String idemKey, String requestHash, int responseStatus, String responseBody,
                                String transactionId, String expiresAt, String createdAt) {
}
