---
title: "Testing money code: concurrency, failure injection and isolated databases"
type: resource
authors: [Suhasini M]
created: 2026-11-27
tags: [testing, concurrency, tdd]
requirements: [TQ-2, TQ-4, TQ-5, NFR-SAFE-01, NFR-SAFE-03, NFR-SAFE-04, NFR-SAFE-05, NFR-TEST-02, NFR-TEST-05]
---

# Testing money code: concurrency, failure injection and isolated databases

## Summary

A financial operation is not proven correct by an HTTP 201. Our Stage 3 tests always check the **stored** balances and ledger entries, end with a reconciliation PASS, and deliberately create the conditions that break naive systems: simultaneous requests, duplicate keys, crashes mid-write and a busy write lock. Distilled from [[test-driven-development-and-concurrency-testing]] plus what we learned while building the suite. Cited by ADR-010 and the `simulate-transfer` skill.

## Patterns that worked

- **Fire together, not one after another.** Submit N HTTP requests from N threads and release them with one `CountDownLatch`, so they really overlap (`ConcurrencyApiTest`).
- **Assert invariants, not exact interleavings.** For 100 concurrent Rs 10 debits on Rs 500 we assert `successes <= 50` and `balance = 50000 - 1000 x successes`, which holds for every valid serial order (FR-CON-04, NFR-SAFE-03).
- **Failure injection with spies.** Replace one repository call with "work once, then throw" (Mockito `@SpyBean`) to simulate a crash after the first entry, after a balance update, or while saving the idempotency key. Then assert nothing at all remains and that a retry with the same key succeeds once (NFR-SAFE-01, `RollbackFailureInjectionTest`, `TransferRollbackTest`).
- **Real crash for durability.** Start the app as a separate JVM, get a 201, `destroyForcibly()` it, restart on the same file, and check the data and reconciliation (NFR-SAFE-04, `DurabilityTest`).
- **Fresh database per Spring context.** Every test context gets its own SQLite *file* under `target/test-db/` (TQ-5). In-memory SQLite was rejected because it only exists inside one connection, which hides locking behaviour (see ADR-010).

## Pitfalls we hit (see docs/testing/defect-log.md)

- **DEF-001:** `src/test/resources/application.properties` *replaces* the main file instead of adding to it. Test overrides now live in `src/test/resources/config/application.properties`.
- **DEF-002:** `${random.uuid}` is re-generated on every property lookup, so reading `${ledger.db.path}` in a test points at a different file. Ask SQLite instead: `PRAGMA database_list`.
- Tests that deliberately corrupt data must run in their own context (`@TestPropertySource` with a unique property) so they cannot break other tests.

## Related

[[sqlite-write-lock-and-driver-behaviour]], [[api-idempotency-and-retry-semantics]], [[financial-reconciliation-and-auditability]]
