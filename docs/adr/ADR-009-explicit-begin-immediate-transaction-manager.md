# ADR-009: Explicit BEGIN IMMEDIATE in a Custom Transaction Manager (refines ADR-002)

## Status
Accepted - refines ADR-002

## Date
2026-11-27

## Context
ADR-002 decided that every money-moving operation runs in one SQLite transaction started with `BEGIN IMMEDIATE`, so the write lock is held before any balance or idempotency key is read (FR-CON-02). It did not say *how* to achieve that through Spring and the sqlite-jdbc driver.

Inspecting sqlite-jdbc 3.45 showed:
- with auto-commit off the driver runs `begin;` straight away, and its `commit()` and `rollback()` immediately start the next transaction in the connection's configured mode;
- so configuring the driver's transaction mode as IMMEDIATE would re-acquire the write lock after every commit while the pooled connection sits idle, and could raise `SQLITE_BUSY` from inside `commit()` after the data was already committed.

Details: [[sqlite-write-lock-and-driver-behaviour]].

## Decision
1. The driver stays in its default DEFERRED mode. Connections are pooled (HikariCP, 8 connections) with WAL, `foreign_keys=ON`, `synchronous=FULL` and `busy_timeout` set on every connection (`DatabaseConfig`).
2. `SqliteImmediateTransactionManager` extends Spring's `DataSourceTransactionManager`. For **read-write** transactions it ends the driver's empty deferred transaction and executes `BEGIN IMMEDIATE`. Read-only transactions keep the deferred begin, which gives reconciliation one consistent WAL snapshot (FR-REC-05).
3. If the lock is not obtained within `busy_timeout`, SQLite returns `SQLITE_BUSY`; `GlobalExceptionHandler` answers `503 SERVICE_BUSY` with `Retry-After: 1`. Nothing was written (FR-CON-06).
4. The idempotency lookup, the business-rule checks, the ledger posting and the stored response all run inside that one transaction (`IdempotencyService`, FR-TRF-07, FR-CON-08).
5. `LedgerService` methods use `Propagation.MANDATORY`, so they cannot run outside such a transaction (FR-CON-01).

## Consequences
- Positive: proven by tests - `BusyTimeoutTest` (503 with no effect, then a successful retry), `ConcurrencyApiTest` (no overdraft, one execution per key), `ReconciliationApiTest` (no false alarms during concurrent writes).
- Positive: reads are never blocked by the writer.
- Negative: all writes in the system are serialised (expected with SQLite; NFR-PERF-02 shows the throughput is sufficient - see the test execution report).
- Negative: relies on a driver detail; a sqlite-jdbc upgrade must re-run `BusyTimeoutTest` and `ConcurrencyApiTest`.

## Citations
- [[sqlite-write-lock-and-driver-behaviour]]
- [[sqlite-concurrency-and-acid-transactions]]
- [[api-idempotency-and-retry-semantics]]
- SRS: FR-CON-01..08, FR-TRF-07, FR-REC-05, DB-02
