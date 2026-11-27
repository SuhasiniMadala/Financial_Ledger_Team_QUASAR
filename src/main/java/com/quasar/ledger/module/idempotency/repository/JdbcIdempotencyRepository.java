package com.quasar.ledger.module.idempotency.repository;

import com.quasar.ledger.module.idempotency.model.IdempotencyRecord;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIdempotencyRepository implements IdempotencyRepository {

    private final JdbcTemplate jdbc;

    public JdbcIdempotencyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<IdempotencyRecord> find(String principalId, String idemKey) {
        return jdbc.query("SELECT * FROM idempotency_record WHERE principal_id = ? AND idem_key = ?",
                (rs, i) -> new IdempotencyRecord(rs.getString("principal_id"), rs.getString("idem_key"), rs.getString("request_hash"),
                        rs.getInt("response_status"), rs.getString("response_body"), rs.getString("transaction_id"),
                        rs.getString("expires_at"), rs.getString("created_at")),
                principalId, idemKey).stream().findFirst();
    }

    @Override
    public void insert(IdempotencyRecord r) {
        jdbc.update("INSERT INTO idempotency_record (principal_id, idem_key, request_hash, response_status, response_body, "
                        + "transaction_id, expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                r.principalId(), r.idemKey(), r.requestHash(), r.responseStatus(), r.responseBody(), r.transactionId(),
                r.expiresAt(), r.createdAt());
    }
}
