# Idempotency in REST APIs

## Summary

Idempotency guarantees that performing an API operation multiple times produces the identical side-effect as performing it a single time.

In financial distributed systems, transient network failures, timeouts, and automated client retries can cause duplicate requests.

Enforcing idempotency prevents duplicate charges or transfers when a client retries a timed-out request.

## How It Works

Consider a user transferring ₹500:

1. The client sends a transfer request with a unique `Idempotency-Key` header.
2. The server processes the transfer, records the outcome, and associates it with the key.
3. The network connection fails before the response reaches the client.
4. The client retries the request using the exact same idempotency key.
5. The server recognizes the key, verifies the payload fingerprint, and returns the stored result with `Idempotent-Replayed: true` instead of processing another transfer.

## Idempotency Key Pattern & Implementation

1. **Client Header**: The client sends a unique UUID in the `Idempotency-Key` header with each mutating request (POST).
2. **Atomic Persistence**: The server computes a SHA-256 fingerprint of the request method, path, and canonical payload, executing the transfer and saving the response atomically in the database.
3. **Duplicate Detection & Replay**:
   - If the same key is received with the exact same fingerprint, the stored response is replayed with header `Idempotent-Replayed: true`.
   - If the same key is reused with a different payload or fingerprint, the server rejects it with `409 IDEMPOTENCY_KEY_REUSED`.
   - Missing headers on financial endpoints are rejected with `400 IDEMPOTENCY_KEY_REQUIRED`.

## Relevance to Our Project

Our FR-2 requirement specifies idempotent fund transfers.

Every mutating financial request must require an `Idempotency-Key` header so that network retries never double-charge an account.

FR-TRF-02 through FR-TRF-04 and NFR-SAFE-05 define the exact idempotency key lifecycle, SHA-256 fingerprinting, replay mechanisms, and 24-hour retention.

## Related Notes

- [[rest-api-security-and-testing]]
- [[database-concurrency-and-locking]]
- [[database-concurrency]]
- [[acid-transactions-and-sqlite]]
- [[acid-transactions]]
- [[double-entry-bookkeeping]]

## Sources

- [Project Problem Statement](../Financial_Wallet_Ledger_Project.pdf)
- [Stripe – Idempotent Requests](https://docs.stripe.com/api/idempotent_requests)
- [IETF Draft – The Idempotency-Key HTTP Header Field](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)
