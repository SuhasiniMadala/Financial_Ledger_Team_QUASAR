package com.quasar.ledger.module.idempotency.service;

/** Everything the idempotency layer needs to know about one incoming request. */
public record IdempotentRequest(String principalId, String key, String method, String path, Object payload, String requestId) {
}
