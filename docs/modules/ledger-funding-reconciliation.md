# Module: Ledger, Idempotency, Funding, Reversals and Reconciliation (Member 2 - Geethika M)

Issues: #9, #10, #11, #12. Requirements: FR-LED-01..10, FR-WAL-07/08, FR-TRF-02..08, FR-CON-01..03/07/08, FR-REC-01..08, FR-ADM-03/07, ACC-01..04, BR-01/02/07..10, NFR-SAFE-01/02/06, NFR-MAIN-02.

## How a money operation runs (all modules use this path)

```
Controller -> Service: validate key format + amount (400, nothing recorded)
           -> IdempotencyService.execute: BEGIN IMMEDIATE (write lock, or 503 after busy timeout)
                 -> look up (caller, key): same fingerprint -> replay stored 201/422; different -> 409
                 -> operation: read accounts, check business rules
                       rule broken -> LedgerService.recordFailed (FAILED, no entries) -> 422 stored
                       ok          -> LedgerService.postCompleted (check debits = credits, write txn,
                                      entries with balance-after, update cached balances) -> 201 stored
                 -> store the idempotency record (same transaction)
              COMMIT  (any exception -> ROLLBACK: nothing at all remains)
```

## Components

| Class | Role |
|---|---|
| `LedgerService` | The only writer of entries and balances (ADR-003). `Propagation.MANDATORY`: refuses to run outside a write transaction. Rejects < 2 entries, debits != credits, currency mismatch, any negative balance - before writing anything (FR-LED-02). |
| `AmountPolicy` | 1 <= amount <= limit (BR-02). |
| `IdempotencyService`, `IdempotencyKeys`, `RequestFingerprint` | ADR-005: key 8-64 `[A-Za-z0-9_-]`, SHA-256 of method + path + canonical body (sorted fields), stores only 201/422 for >= 24 h. |
| `FundingService` | Admin test funding: DEBIT `SYS-FUND-<currency>`, CREDIT an ACTIVE user wallet (ACC-03). |
| `ReversalService` | Mirror-image REVERSAL linked to the original (ACC-04); FAILED/REVERSAL -> 422 NOT_REVERSIBLE; twice -> 409 ALREADY_REVERSED; DEACTIVATED wallet -> 422; would go negative -> 422 INSUFFICIENT_FUNDS; FROZEN allowed (BR-07). |
| `ReconciliationService` | One read-only snapshot: C1 per-currency debits = credits, C2 every COMPLETED txn balanced with >= 2 entries (and FAILED has none), C3 stored = derived balance (wallet: credits - debits; system: debits - credits). Never repairs. |
| `AdminLedgerQueryService`, `AdminStatisticsService` | FR-ADM-03 listings with filters; FR-ADM-07 statistics. |

## Tests

`LedgerServiceTest` (5), `LedgerImmutabilityTest` (3), `IdempotencyRulesTest` (10), `TestFundingApiTest` (8), `ReversalApiTest` (5), `RollbackFailureInjectionTest` (3: crash after first entry, after balance update, at idempotency insert), `ReconciliationApiTest` (3, incl. snapshot under concurrent writes), `ReconciliationDetectsCorruptionTest` (2), `AdminLedgerQueryApiTest` (3), `BrainVaultTest` (4).

Knowledge: [[sqlite-write-lock-and-driver-behaviour]], [[double-entry-accounting-principles]], [[financial-reconciliation-and-auditability]].
