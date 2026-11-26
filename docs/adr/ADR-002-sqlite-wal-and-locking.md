# ADR-002: SQLite Database with Write-Ahead Logging (WAL) and BEGIN IMMEDIATE Locking

## Status
Accepted

## Date
2026-11-26

## Context
Requirement FR-3 demands that multiple simultaneous transfer requests on the same wallet be handled safely without race conditions or balance corruption. In naive multi-threaded web applications, concurrent requests read the same balance and overwrite each other (lost updates) or overdraw accounts.

We considered:
1. **Distributed Lock Server (Redis / ZooKeeper)**: External lock coordinators.
2. **Optimistic Locking**: Version columns with retry loops on commit failure.
3. **Pessimistic In-Database Locking via SQLite's `BEGIN IMMEDIATE` and WAL Mode**: Using SQLite's built-in single-writer transaction serialization.

An external lock coordinator violates the zero-configuration, single-file serverless database principle encouraged in the Project Statement. Optimistic locking under high contention causes excessive transaction aborts and complex replay cascades.

## Decision
We use **SQLite in Write-Ahead Logging (WAL) mode** combined with **`BEGIN IMMEDIATE` transaction locking**:
1. All financial write operations (test funding, transfers, payments, reversals) execute within single SQLite transactions initiated with `BEGIN IMMEDIATE`.
2. `BEGIN IMMEDIATE` acquires the reserved write lock before reading any balances or idempotency records, guaranteeing that writers are strictly serialized at the database layer.
3. Concurrent readers run concurrently without blocking writers using WAL mode snapshots.
4. If a write lock cannot be obtained within the configured `busy_timeout` (default 5,000 ms), the application cleanly returns HTTP 503 `SERVICE_BUSY` with a `Retry-After` header.

Mandatory PRAGMA settings:
```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = FULL;
PRAGMA busy_timeout = 5000;
PRAGMA foreign_keys = ON;
```

## Consequences
- **Positive**:
  - Eliminates lost updates, negative balance race conditions, and dirty reads at the database engine level.
  - Zero external infrastructure or lock server dependencies.
  - Non-blocking reads for balances, transaction histories, and reconciliation.
- **Negative**:
  - Write throughput is capped at the speed of SQLite's serialized single writer (sufficient for the target demonstration requirement of $\ge 10$ transfers/sec under NFR-PERF-02).
  - Exactly one application process may connect to the database file; cannot run multiple clustered server instances against one SQLite file.

## Citations
- Second Brain Reference: [[sqlite-concurrency-and-acid-transactions]]
- SRS Traceability: FR-3, FR-CON-01, FR-CON-02, FR-CON-03, DB-01, DB-02
