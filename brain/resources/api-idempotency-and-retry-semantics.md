# API Idempotency and Retry Semantics

## Summary

In distributed systems and network communication, network failures, socket timeouts, and client disconnects are inevitable. If a client attempts to transfer funds and experiences a timeout, it cannot determine whether the request failed before reaching the server, was executed successfully, or failed during response transmission.

Idempotency guarantees that executing the same operation multiple times results in the exact same state and response as executing it once.

## Idempotency Key Specification & Request Lifecycle

1. **Header Requirement**: Mutating financial endpoints (`POST /transfers`, `POST /payments`, `POST /admin/wallets/{id}/test-funding`, `POST /admin/transactions/{id}/reversal`) mandate an `Idempotency-Key` header.
2. **Key Constraints**: 8 to 64 alphanumeric characters, `-`, and `_`.
3. **Payload Fingerprinting**: A cryptographic SHA-256 hash is computed over the canonical request method, target path, and request body:
   $$\text{Fingerprint} = \text{SHA-256}(\text{Method} + \text{Path} + \text{CanonicalBody})$$
4. **Lifecycle States & Outcomes**:
   - **First Arrival**: The transaction executes. The response status (201 or 422) and response body are committed inside the exact same SQLite database transaction as the financial ledger entries.
   - **Duplicate Retry (Same Fingerprint)**: The stored response is replayed immediately with header `Idempotent-Replayed: true`. No new transaction or ledger entry is created.
   - **Key Conflict (Different Fingerprint)**: The client sent the same key with altered payload data. Rejected immediately with `409 IDEMPOTENCY_KEY_REUSED`.
   - **Concurrent Duplicate Arrival**: If two identical requests arrive simultaneously with the same key, SQLite's write lock forces one to execute first while the second waits, finds the committed record, and returns the replayed response.

## Database Schema Representation

```sql
CREATE TABLE idempotency_record (
    principal_id     TEXT NOT NULL,
    idem_key         TEXT NOT NULL,
    request_hash     TEXT NOT NULL,
    response_status  INTEGER NOT NULL,
    response_body    TEXT NOT NULL,
    transaction_id   TEXT,
    expires_at       TEXT NOT NULL,
    created_at       TEXT NOT NULL,
    PRIMARY KEY (principal_id, idem_key),
    FOREIGN KEY (principal_id) REFERENCES user_account(id),
    FOREIGN KEY (transaction_id) REFERENCES transaction_record(id)
);
```

## Relevance to Project Requirements

- **FR-2**: Idempotent fund transfers preventing duplicate charges upon network retries.
- **FR-TRF-02 to FR-TRF-04**: Define key format, SHA-256 request hashing, and 24-hour retention.
- **NFR-SAFE-05**: Formal test assertion for $N$ identical concurrent or sequential retries yielding exactly one effect.

## Related Notes

- [[sqlite-concurrency-and-acid-transactions]]
- [[double-entry-accounting-principles]]
- [[api-security-and-input-validation]]

## Sources

- IETF Internet-Draft: *The Idempotency-Key HTTP Header Field* (draft-ietf-httpapi-idempotency-key-header)
- Stripe Engineering Blog: *Designing Robust and Predictable APIs with Idempotency*
- Financial Ledger SRS v1.4, Section 4.3 & Figure B-4
