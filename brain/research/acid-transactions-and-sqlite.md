---
title: "ACID Transactions and SQLite"
type: research
authors: [Akshaya A]
created: 2026-11-26
---

# ACID Transactions and SQLite

## Summary

ACID (Atomicity, Consistency, Isolation, Durability) guarantees ensure reliable database transaction processing even amid system crashes or concurrent requests.

SQLite provides built-in serverless ACID transactions using Write-Ahead Logging (WAL) and rollback journals.

Financial operations must maintain ACID compliance to avoid partial balance updates or corrupted ledger states.

## ACID Properties in Financial Ledgers

- **Atomicity**: An operation (updating source balance, updating destination balance, writing two ledger entries, inserting idempotency record) must commit completely or roll back entirely.
- **Consistency**: Database invariants, foreign keys, positive amounts, and debit/credit balance checks are preserved across transactions.
- **Isolation**: Simultaneous operations on the same wallet execute without race conditions or intermediate dirty reads.
- **Durability**: Once a transaction is committed, changes survive unexpected application termination or power failure.

## SQLite Transaction Modes

- `BEGIN DEFERRED`: The transaction starts in read mode and escalates to write lock on the first mutating SQL statement. This can lead to `SQLITE_BUSY` deadlocks.
- `BEGIN IMMEDIATE`: Immediately acquires the write lock on the database before reading balances, ensuring no other write transaction can start.

## Relevance to Our Project

Requirement FR-3 and NFR-SAFE-01 to NFR-SAFE-04 demand full ACID transactions.

DB-01 and DB-02 require single SQLite file persistence with WAL mode and `synchronous=FULL`.

## Related Notes

- [[database-concurrency-and-locking]]
- [[sqlite-wal-mode-and-busy-timeout]]
- [[immutable-audit-trails]]
- [[double-entry-bookkeeping]]

## Sources

- [SQLite – Transaction Documentation](https://www.sqlite.org/lang_transaction.html)
- [SQLite – Locking and Concurrency](https://www.sqlite.org/lockingv3.html)
- [Project Statement: Financial Ledger & Digital Wallet API](../Financial_Wallet_Ledger_Project.pdf)
