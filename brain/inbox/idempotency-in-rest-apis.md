# Idempotency in REST APIs

## Summary

Idempotency guarantees that performing an API operation multiple times produces the identical side-effect as performing it a single time.

In financial distributed systems, transient network failures, timeouts, and automated client retries can cause duplicate requests.

Enforcing idempotency prevents duplicate charges or transfers when a client retries a timed-out request.

## Idempotency Key Pattern

1. **Client Header**: The client sends a unique UUID in the `Idempotency-Key` header with each mutating request (POST).
2. **Atomic Persistence**: The server computes a SHA-256 fingerprint of the request method, path, and canonical payload, executing the transfer and saving the response atomically in the database.
3. **Duplicate Detection & Replay**:
   - If the same key is received with the exact same fingerprint, the stored response is replayed with header `Idempotent-Replayed: true`.
   - If the same key is reused with a different payload or fingerprint, the server rejects it with `409 IDEMPOTENCY_KEY_REUSED`.
   - Missing headers are rejected with `400 IDEMPOTENCY_KEY_REQUIRED`.

## Relevance to Our Project

Requirement FR-2 requires idempotent fund transfers.

FR-TRF-02 through FR-TRF-04 and NFR-SAFE-05 define the exact idempotency key lifecycle, SHA-256 fingerprinting, replay mechanisms, and 24-hour retention.

## Related Notes

- [[rest-api-security-and-testing]]
- [[database-concurrency-and-locking]]
- [[acid-transactions-and-sqlite]]
- [[double-entry-bookkeeping]]

## Sources

- [IETF Draft – The Idempotency-Key HTTP Header Field](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)
- [Stripe API – Idempotent Requests](https://stripe.com/docs/api/idempotent_requests)
- [Project Statement: Financial Ledger & Digital Wallet API](../Financial_Wallet_Ledger_Project.pdf)
