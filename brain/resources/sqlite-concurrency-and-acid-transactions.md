# SQLite Concurrency, Locking, and ACID Transactions

## Summary

In a financial digital wallet, concurrency anomalies (lost updates, dirty reads, negative balance race conditions, and phantom commits) cause unacceptable monetary corruption. 

SQLite provides robust, serverless ACID guarantees through Write-Ahead Logging (WAL) and explicit transaction locking modes. By understanding SQLite's file-level locking architecture, the application serializes financial write operations without requiring heavy distributed locking infrastructure.

## SQLite Locking Mechanics & Transaction Modes

SQLite utilizes internal lock states (`UNLOCKED`, `SHARED`, `RESERVED`, `PENDING`, `EXCLUSIVE`). In default `DEFERRED` mode, transactions begin with a shared read lock and upgrade to a write lock only upon encountering a mutating SQL statement (`INSERT`, `UPDATE`). When two concurrent transactions attempt to upgrade simultaneously, deadlock occurs, causing immediate `SQLITE_BUSY` errors.

### The `BEGIN IMMEDIATE` Serialization Pattern

To prevent deadlock and race conditions during balance-altering requests:
1. The transaction executes `BEGIN IMMEDIATE`.
2. This immediately acquires a `RESERVED` lock on the database file.
3. Only **one** writer can hold this lock at any instant; other writers wait until the lock is released or until the `busy_timeout` expires.
4. Because the write lock is secured before reading balances or idempotency records, concurrent balance checks and deductions are strictly serialized.

```
Incoming Request
       │
       ▼
BEGIN IMMEDIATE ──► Acquired? ──No (Timeout)──► 503 SERVICE_BUSY (Retry-After)
       │ Yes
       ▼
Verify Idempotency Key & Read Balances
       │
Check Balance Sufficiency (balance >= amount)
       │ Passed? ──No──► Write FAILED Txn + Save Idempotency ──► COMMIT ──► 422
       │ Yes
       ▼
Insert COMPLETED Txn + Ledger Entries (Debit & Credit)
       │
Update Cached Balances (Assert sum(debits) == sum(credits))
       │
Save Idempotency Record (Payload hash + HTTP 201 response)
       │
     COMMIT ──► Release Lock ──► 201 CREATED
```

## Write-Ahead Logging (WAL) Architecture

Under `PRAGMA journal_mode = WAL`:
- Mutating operations append changed database pages to an auxiliary `.db-wal` file.
- **Non-blocking Reads**: Readers read from a consistent database snapshot using the WAL index (`.db-shm`), completely unhindered by an active writer.
- **Single Serialized Writer**: Exactly one process writes to the WAL log at a time.
- Periodic checkpoints transfer committed WAL pages back into the main `.db` database file.

## Mandatory SQLite Database Configuration

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = FULL;
PRAGMA busy_timeout = 5000;
PRAGMA foreign_keys = ON;
```

## Relevance to Project Requirements

- **FR-3**: Safe concurrent balance mutability without race conditions or balance corruption.
- **FR-CON-01 to FR-CON-05**: Mandates `BEGIN IMMEDIATE` transactions, strict serialization, and 503 busy handling.
- **DB-01 & DB-02**: Mandates single local SQLite file, WAL mode, foreign keys, and synchronous full durability.

## Related Notes

- [[double-entry-accounting-principles]]
- [[api-idempotency-and-retry-semantics]]
- [[immutable-ledger-and-audit-trails]]
- [[financial-reconciliation-and-auditability]]

## Sources

- SQLite Official Documentation: *Write-Ahead Logging* (https://www.sqlite.org/wal.html)
- SQLite Official Documentation: *File Locking and Concurrency in SQLite Version 3* (https://www.sqlite.org/lockingv3.html)
- Financial Ledger SRS v1.4, Section 4.5 & Section 6.1
