
# Idempotency in REST APIs

## Summary

Idempotency means that repeating the same request produces the same intended result as executing it once.

In financial applications, network failures may cause clients to retry requests even when the original request was already processed.

An idempotency key helps the server identify repeated requests and prevents duplicate financial operations.

## How It Works

Consider a user transferring ₹500.

1. The client sends a transfer request with an idempotency key.
2. The server processes the transfer and records the result.
3. The network connection fails before the response reaches the client.
4. The client retries the request using the same key.
5. The server recognizes the key and returns the previously recorded result instead of processing another transfer.

## Relevance to Our Project

Our FR-2 requirement specifies idempotent transfers.

Every transfer request must contain an idempotency key so that network retries do not result in duplicate charges.

This helps protect wallet balances and ensures reliable transfer processing.

## Related Notes

- [[ACID Transactions]]
- [[Double-Entry Bookkeeping]]
- [[Database Concurrency]]

## Sources

- [Stripe – Idempotent Requests](https://docs.stripe.com/api/idempotent_requests)
- [Project Problem Statement](../Financial_Wallet_Ledger_Project.pdf)
