---
title: "SQLite write lock in practice: BEGIN IMMEDIATE, busy timeout and sqlite-jdbc behaviour"
type: resource
authors: [Geethika M]
created: 2026-11-27
tags: [sqlite, concurrency, transactions, jdbc]
requirements: [FR-CON-01, FR-CON-02, FR-CON-03, FR-CON-06, FR-CON-08, FR-TRF-07, DB-02]
---

# SQLite write lock in practice

## Summary

SQLite has exactly one writer at a time. A money operation is only safe if it takes that write lock **before** it reads any balance or idempotency key, and keeps it until COMMIT. `BEGIN IMMEDIATE` does exactly that; a plain `BEGIN` (DEFERRED) only takes the lock at the first write, which is too late. This note records what we learned making that work through Spring and the sqlite-jdbc driver in Stage 3. It refines [[sqlite-concurrency-and-acid-transactions]] and is cited by ADR-009.

## Rules we rely on

1. **Lock first, then read.** Two transfers that both read "balance = Rs 500" before either writes would both pass the funds check (lost update / overdraft). With BEGIN IMMEDIATE the second request waits; when it continues it reads the committed balance (FR-CON-02, FR-CON-03).
2. **Idempotency lookup inside the lock.** The same applies to the key table: two identical requests must not both see "key not used yet" ([[api-idempotency-and-retry-semantics]], FR-CON-08). The key, the transaction, the entries and the balances commit together (FR-TRF-07).
3. **Busy timeout = 503, not a hang.** `busy_timeout` (5000 ms) bounds the wait. When it expires SQLite returns `SQLITE_BUSY` (code 5); we roll back, change nothing, and answer `503 SERVICE_BUSY` with `Retry-After: 1` so the client retries with the same key (FR-CON-06).
4. **Readers are not blocked** in WAL mode; a read-only transaction (`BEGIN` deferred) sees one consistent snapshot, which reconciliation needs (FR-REC-05, [[financial-reconciliation-and-auditability]]).

## Driver behaviour we discovered (sqlite-jdbc 3.45)

- With auto-commit off, the driver issues `begin;` immediately, and its `commit()` and `rollback()` **immediately start the next transaction** using the connection's configured mode (checked by disassembling `SQLiteConnection.commit`).
- Therefore setting the connection-wide mode to IMMEDIATE would grab the write lock again right after every commit, while the connection sits idle - and could even throw SQLITE_BUSY *after* a successful commit.
- Our solution (ADR-009): keep the driver in DEFERRED mode and, in a small Spring `DataSourceTransactionManager` subclass, replace the empty deferred transaction with `BEGIN IMMEDIATE` for read-write transactions only. Read-only transactions keep the deferred snapshot.

## How it is verified

- `BusyTimeoutTest`: another connection holds BEGIN IMMEDIATE; a transfer gets 503 with nothing recorded; the retry with the same key succeeds once.
- `ConcurrencyApiTest`: 2 x Rs 400 from Rs 500 -> exactly one succeeds; 100 concurrent debits never overdraw; 10 concurrent identical requests -> one transaction.
- `ReconciliationApiTest.snapshotIsConsistentUnderConcurrentWrites`: no false discrepancies while writes commit.

## Related

[[sqlite-transactions-and-wal-mode]], [[sqlite-wal-mode-and-busy-timeout]], [[database-locking-and-race-conditions]], [[testing-concurrency-and-failure-injection]]
