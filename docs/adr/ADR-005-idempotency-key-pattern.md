# ADR-005: Idempotency Key Pattern with SHA-256 Request Fingerprinting

## Status
Accepted

## Date
2026-11-26

## Context
Transient network interruptions frequently prevent HTTP responses from reaching clients after a server has successfully processed a transaction. Clients configured to retry upon timeout risk triggering duplicate charges or transfers.

We evaluated:
1. **No Idempotency Control**: Rely on client confirmation before retrying.
2. **Simple Key Deduplication**: Store key in-memory or in cache with a boolean flag.
3. **Persistent Idempotency Record with Request Fingerprinting & Atomic Commit**: Persisting idempotency keys, SHA-256 payload hashes, and complete responses in the database alongside the financial transaction.

## Decision
We implement a **persistent Idempotency Key pattern with SHA-256 fingerprinting**:
1. All mutating financial endpoints require an `Idempotency-Key` HTTP header (8–64 characters, alphanumeric, `-`, `_`).
2. The server computes a SHA-256 hash of the canonical request method, path, and body.
3. In the same database transaction as the financial transfer, an idempotency record is inserted storing `principal_id`, `idem_key`, `request_hash`, `response_status`, and `response_body`.
4. Subsequent retries:
   - **Matching Key & Matching Hash**: Replay the stored HTTP status and body with header `Idempotent-Replayed: true`. No new transaction is processed.
   - **Matching Key & Different Hash**: Reject immediately with HTTP 409 `IDEMPOTENCY_KEY_REUSED`.
   - **Missing Key**: Reject immediately with HTTP 400 `IDEMPOTENCY_KEY_REQUIRED`.
5. Records are retained for at least 24 hours.

## Consequences
- **Positive**:
  - Safe network retries without fear of double-charging accounts.
  - Detects payload tampering or accidental key reuse across different operations.
  - Guaranteed atomicity between the financial transaction outcome and its idempotency record.
- **Negative**:
  - Requires extra database storage for response payloads and indexes on `(principal_id, idem_key)`.

## Citations
- Second Brain Reference: [[api-idempotency-and-retry-semantics]]
- SRS Traceability: FR-2, FR-TRF-02 to FR-TRF-04, DB-04, NFR-SAFE-05, Figure B-4
