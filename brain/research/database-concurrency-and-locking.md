---
title: "Database Concurrency and Locking"
type: research
authors: [Akshaya A]
created: 2026-11-26
---

# Database Concurrency and Locking

## Summary

Concurrency anomalies like race conditions, dirty reads, and lost updates pose severe risks in financial systems.

When two outgoing transfers execute concurrently from the same wallet, both might read an initial balance of ₹500 and attempt to deduct ₹400, resulting in an illegal negative balance if uncoordinated.

Proper locking and serialization mechanisms ensure that concurrent balance mutability remains mathematically consistent.

## Concurrency Hazards

1. **Lost Updates**: Two concurrent threads read balance $$B$$, calculate $$B - x$$ and $$B - y$$, and overwrite each other's result.
2. **Race Conditions & Overdrafts**: Two requests pass the balance sufficiency check concurrently before either commits a debit, leading to negative balances.
3. **Double-Spend**: Exploiting network latency to initiate duplicate transfers from an account before the balance deduction is visible.

## Solutions in SQLite

- Using `BEGIN IMMEDIATE` transaction locking serializes writers at the database layer.
- SQLite allows single-writer serialization while WAL mode allows non-blocking readers.
- In-memory or database-level busy handler retries during lock contention, returning HTTP 503 `SERVICE_BUSY` if the timeout expires.

## Relevance to Our Project

FR-3 (Concurrent Balance Mutability) and FR-CON-01 to FR-CON-05 require handling simultaneous transfers safely without balance corruption.

Negative test requirements TQ-4 test race safety with concurrent transfers.

## Related Notes

- [[acid-transactions-and-sqlite]]
- [[sqlite-wal-mode-and-busy-timeout]]
- [[rest-api-security-and-testing]]
- [[financial-ledger-reconciliation]]

## Sources

- [Martin Kleppmann – Designing Data-Intensive Applications: Concurrency Control](https://dataintensive.net/)
- [SQLite – Locking in SQLite](https://www.sqlite.org/lockingv3.html)
- [Project Statement: Financial Ledger & Digital Wallet API](../Financial_Wallet_Ledger_Project.pdf)
