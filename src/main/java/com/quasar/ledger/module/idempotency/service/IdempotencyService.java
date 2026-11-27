package com.quasar.ledger.module.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.util.Timestamps;
import com.quasar.ledger.common.web.ErrorResponseFactory;
import com.quasar.ledger.module.idempotency.model.IdempotencyRecord;
import com.quasar.ledger.module.idempotency.repository.IdempotencyRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a money operation exactly once per (caller, Idempotency-Key) - ADR-005, FR-TRF-02..08, FR-CON-08.
 *
 * <p>Order inside ONE read-write database transaction that starts with BEGIN IMMEDIATE:
 * <ol>
 *   <li>look up the key (the write lock is already held, so a concurrent duplicate waits here);</li>
 *   <li>same fingerprint: return the stored status and body with Idempotent-Replayed (no new transaction);</li>
 *   <li>different fingerprint: 409 IDEMPOTENCY_KEY_REUSED, nothing changes;</li>
 *   <li>otherwise run the operation and store its 201/422 outcome in the same transaction (FR-TRF-07).</li>
 * </ol>
 * If the operation throws, the transaction rolls back: no record, no entry, no balance change and no
 * stored key, so retrying with the same key is safe (SRS 4.3.2 scenario 7).
 */
@Service
public class IdempotencyService {

    /** FR-TRF-08: records are kept at least 24 hours. They are never deleted by this release. */
    static final Duration RETENTION = Duration.ofHours(24);

    private final IdempotencyRepository repository;
    private final RequestFingerprint fingerprint;
    private final TransactionTemplate writeTransaction;
    private final ErrorResponseFactory errors;
    private final ObjectMapper json;
    private final Clock clock;

    public IdempotencyService(IdempotencyRepository repository, RequestFingerprint fingerprint, TransactionTemplate writeTransaction,
                              ErrorResponseFactory errors, ObjectMapper json, Clock clock) {
        this.repository = repository;
        this.fingerprint = fingerprint;
        this.writeTransaction = writeTransaction;
        this.errors = errors;
        this.json = json;
        this.clock = clock;
    }

    public IdempotentResponse execute(IdempotentRequest request, Supplier<FinancialOutcome> operation) {
        IdempotencyKeys.validate(request.key());
        String hash = fingerprint.of(request.method(), request.path(), request.payload());
        return writeTransaction.execute(status -> {
            Optional<IdempotencyRecord> existing = repository.find(request.principalId(), request.key());
            if (existing.isPresent()) {
                IdempotencyRecord stored = existing.get();
                if (!stored.requestHash().equals(hash)) {
                    throw new ApiException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                            "This Idempotency-Key was already used with a different request");
                }
                return new IdempotentResponse(stored.responseStatus(), stored.responseBody(), true);
            }
            FinancialOutcome outcome = operation.get();
            String body = outcome.status() == 201
                    ? toJson(outcome.body())
                    : toJson(errors.create(outcome.errorCode(), outcome.message(), request.path(), request.requestId(),
                            outcome.transactionId()));
            Instant now = clock.instant();
            repository.insert(new IdempotencyRecord(request.principalId(), request.key(), hash, outcome.status(), body,
                    outcome.transactionId(), Timestamps.format(now.plus(RETENTION)), Timestamps.format(now)));
            return new IdempotentResponse(outcome.status(), body, false);
        });
    }

    private String toJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // Throwing rolls back the whole operation: we never commit money without its stored response.
            throw new IllegalStateException("Cannot serialise response", e);
        }
    }
}
