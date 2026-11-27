package com.quasar.ledger.module.idempotency.repository;

import com.quasar.ledger.module.idempotency.model.IdempotencyRecord;
import java.util.Optional;

/** Persistence port for idempotency records (NFR-MAIN-03). */
public interface IdempotencyRepository {

    Optional<IdempotencyRecord> find(String principalId, String idemKey);

    void insert(IdempotencyRecord record);
}
