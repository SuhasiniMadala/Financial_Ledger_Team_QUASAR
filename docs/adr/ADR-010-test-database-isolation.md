# ADR-010: A Fresh SQLite File per Test Context (not in-memory SQLite)

## Status
Accepted

## Date
2026-11-27

## Context
TQ-5 / NFR-TEST-05 require tests to run on "an isolated in-memory or fresh database on every run, including CI". The SRS also requires concurrency tests (TQ-4, NFR-SAFE-03/05), rollback tests (TQ-2) and a durability test (NFR-SAFE-04).

An in-memory SQLite database (`jdbc:sqlite::memory:`) exists only inside one JDBC connection. To share it, the pool must be limited to one connection, which serialises everything in Java before SQLite ever sees a second writer. Concurrency tests would then pass even if the locking design were wrong, and WAL mode, busy timeouts and crash recovery could not be tested at all.

## Decision
1. Every Spring test context uses a new SQLite **file**: `ledger.db.path=target/test-db/ledger-${random.uuid}.db` in `src/test/resources/config/application.properties`. The file is created fresh, in WAL mode, with the same pool size and settings as production.
2. Test classes share a context (and file) when their configuration is identical; tests therefore use unique synthetic users and assert on their own wallets. Tests that deliberately corrupt data (`ReconciliationDetectsCorruptionTest`) or change settings (`BusyTimeoutTest`, failure-injection spies) get their own context.
3. Concurrency tests send real HTTP requests to the embedded server; the durability test starts a separate JVM on its own file.
4. `mvn clean` deletes all test databases; nothing persists between runs.

## Consequences
- Positive: tests exercise the real locking, WAL and durability behaviour that production uses ([[testing-concurrency-and-failure-injection]]).
- Positive: no shared state between runs or between developers; CI starts from an empty `target/`.
- Negative: a few seconds slower than in-memory; the full suite takes about 1-2 minutes.
- Pitfall recorded as DEF-002: `${random.uuid}` is re-generated on every lookup, so tests must ask SQLite (`PRAGMA database_list`) for the real file path.

## Citations
- [[testing-concurrency-and-failure-injection]]
- [[sqlite-concurrency-and-acid-transactions]]
- SRS: TQ-2, TQ-4, TQ-5, NFR-TEST-05, NFR-SAFE-03, NFR-SAFE-04
