# Requirements Traceability Matrix (SE-5)

Requirement -> GitHub Issue -> Pull Request -> Test -> Skill -> Brain note. Updated for Stage 3 (2026-11-27).

**Pull requests.** The Stage 3 PRs are opened by their owners in the order M1 -> M2 -> M3 (`docs/branching-strategy.md`). The PR column names the PR each row comes from; each owner replaces `#?` with the real number after opening the PR. Earlier work: PR #1-#2 (Stage 1 research), #3 (Stage 1 setup), #4 (Stage 2 design).

| PR label | Branch | Owner | Number |
|---|---|---|---|
| M1 PR | `stage3/member-1-account` | Akshaya A | #23 |
| M2 PR | `stage3/member-2-payments-ledger` | Geethika M | #24 |
| M3 PR | `stage3/member-3-transfers-history` | Suhasini M | #25 |

## Functional requirements

| Requirement | Issue | PR | Tests | Skill | Brain note / ADR |
|---|---|---|---|---|---|
| FR-AUTH-01..04 registration, duplicate email, password length | #5 | M1 PR | `AuthApiTest` | local-check | [[api-security-and-input-validation]], ADR-006 |
| FR-AUTH-05 admin only from configuration | #5 | M1 PR | `AuthApiTest.cannotRegisterAdminThroughApi`, `configuredAdminCanLogIn` | - | [[jwt-authentication-and-role-based-authorization]] |
| FR-AUTH-06/07 JWT, generic 401, bad tokens | #5 | M1 PR | `AuthApiTest`, `JwtServiceTest` | - | [[api-error-contract-and-access-control]], ADR-006 |
| FR-AUTH-08 roles per Table 3-1 | #5 | M1 PR | `SecurityAccessMatrixTest` | - | [[api-error-contract-and-access-control]] |
| FR-AUTH-09 not yours = 404 | #5 | M1 PR, M3 PR | `WalletApiTest.otherUsersWalletIsNotFound`, `TransferApiTest.sourceMustBeOwned`, `TransactionHistoryApiTest.accessAndFilters` | - | [[api-error-contract-and-access-control]] |
| FR-WAL-01..04 create wallet, one per currency, starts at zero | #5 | M1 PR | `WalletApiTest`, `AuthApiTest.registersMerchantWithCodeAndWallet` | simulate-transfer (step 1) | ADR-008, [[monetary-representation-and-integer-units]] |
| FR-WAL-05 balance changes only with balanced entries | #9 | M2 PR | `LedgerServiceTest`, `ArchitectureRulesTest.onlyLedgerWritesEntriesAndBalances`, reconciliation C3 | simulate-transfer (step 5) | [[double-entry-accounting-principles]], ADR-003 |
| FR-WAL-06 list own wallets | #5 | M1 PR | `WalletApiTest.listsOwnWalletsOnly` | - | - |
| FR-WAL-07/08 test funding, idempotent | #9 | M2 PR | `TestFundingApiTest` | simulate-transfer (step 1) | [[double-entry-accounting-principles]] |
| FR-TRF-01 transfer with description | #13 | M3 PR | `TransferApiTest.successfulTransfer`, `descriptionIdsAndRoles` | simulate-transfer E01 | - |
| FR-TRF-02/03 key required, key format | #9 | M2 PR | `IdempotencyRulesTest`, `TestFundingApiTest.keyIsRequiredAndValidated`, `TransferApiTest.missingKey` | simulate-transfer E12 | [[api-idempotency-and-retry-semantics]], ADR-005 |
| FR-TRF-04..08 fingerprint, replay, 409, same transaction, store 201/422 | #9 | M2 PR | `IdempotencyRulesTest.fingerprint`, `TransferApiTest.replay`, `reusedKey`, `insufficientFunds`, `RollbackFailureInjectionTest.failureAtIdempotencyInsert` | simulate-transfer step 3, E09, E10 | [[api-idempotency-and-retry-semantics]], ADR-005 |
| FR-TRF-09 amount 1..limit, integer | #9, #13 | M2 PR, M3 PR | `IdempotencyRulesTest.amountBoundaries`, `TransferApiTest.invalidAmounts`, `TestFundingApiTest.amountBoundaries` | simulate-transfer E03-E05, E11 | [[monetary-representation-and-integer-units]], ADR-004, ADR-008 |
| FR-TRF-10 missing / foreign source = 404 | #13 | M3 PR | `TransferApiTest.sourceMustBeOwned` | simulate-transfer E06 | [[api-error-contract-and-access-control]] |
| FR-TRF-11/12 business-rule rejections (422) | #13 | M3 PR | `TransferApiTest.insufficientFunds`, `invalidDestinations`, `otherBusinessRules` | simulate-transfer E02, E07, E08, E13 | [[double-entry-accounting-principles]] |
| FR-TRF-13 FAILED record for rejections | #13 | M3 PR | `TransferApiTest.insufficientFunds`, `PaymentAndMerchantApiTest.paymentRejections` | simulate-transfer E02 | [[immutable-ledger-and-audit-trails]] |
| FR-TRF-14 one transaction: record, debit, credit, balances | #13 | M3 PR | `TransferApiTest.successfulTransfer` | simulate-transfer step 4 | [[double-entry-accounting-principles]] |
| FR-PAY-01..03 merchant payments | #13 | M3 PR | `PaymentAndMerchantApiTest` | - | [[double-entry-accounting-principles]] |
| FR-PAY-04..07 merchant account, list, privacy, statistics | #13 | M3 PR | `PaymentAndMerchantApiTest` (incl. `statisticsCountReversedPaymentsAsCompleted`), `MerchantAverageTest` | - | [[api-error-contract-and-access-control]] |
| FR-CON-01..03 one transaction, lock first, check + debit together | #9 | M2 PR | `LedgerServiceTest.requiresSurroundingTransaction`, `ConcurrencyApiTest` | simulate-transfer step 4 | [[sqlite-write-lock-and-driver-behaviour]], ADR-002, ADR-009 |
| FR-CON-04/05 serial equivalence, never negative | #14 | M3 PR | `ConcurrencyApiTest.hundredConcurrentDebits`, `incomingAndOutgoingTogether`, `LedgerServiceTest.otherInvariantViolationsAreRejected` | simulate-transfer step 4 | [[sqlite-concurrency-and-acid-transactions]] |
| FR-CON-06 busy timeout -> 503 | #14 | M3 PR | `BusyTimeoutTest` | - | [[sqlite-write-lock-and-driver-behaviour]], ADR-009 |
| FR-CON-07 full rollback on failure | #9, #13 | M2 PR, M3 PR | `RollbackFailureInjectionTest`, `TransferRollbackTest` | simulate-transfer E15 (covered by test) | [[testing-concurrency-and-failure-injection]] |
| FR-CON-08 concurrent same key executes once | #14 | M3 PR | `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` | simulate-transfer step 3 | [[api-idempotency-and-retry-semantics]] |
| FR-LED-01..04 entries, balance check, FAILED no entries, entry fields | #9 | M2 PR | `LedgerServiceTest`, `LedgerImmutabilityTest.noEntriesForFailedTransactions` | - | [[double-entry-accounting-principles]], ADR-003 |
| FR-LED-05/06 immutable entries and transactions | #9 | M2 PR | `LedgerImmutabilityTest` | - | [[immutable-ledger-and-audit-trails]], ADR-003 |
| FR-LED-07..10 reversals | #9 | M2 PR | `ReversalApiTest` | - | [[immutable-ledger-and-audit-trails]] |
| FR-LED-11/12 user history and detail | #13 | M3 PR | `TransactionHistoryApiTest` | simulate-transfer step 3 | [[api-error-contract-and-access-control]] |
| FR-REC-01..08 reconciliation C1-C3, snapshot, read-only | #10 | M2 PR | `ReconciliationApiTest`, `ReconciliationDetectsCorruptionTest` | simulate-transfer step 5 | [[financial-reconciliation-and-auditability]], ADR-007 |
| FR-ADM-01/02 users, merchants, accounts | #5 | M1 PR | `AdminDirectoryApiTest` | - | - |
| FR-ADM-03 transactions and entries | #10 | M2 PR | `AdminLedgerQueryApiTest` | - | [[immutable-ledger-and-audit-trails]] |
| FR-ADM-04..06 status transitions, audit, no balance edits | #5 | M1 PR | `AdminDirectoryApiTest`, `LedgerImmutabilityTest.walletStatusAuditIsAppendOnly` | simulate-transfer E13 | [[api-error-contract-and-access-control]] |
| FR-ADM-07 statistics | #10 | M2 PR | `AdminLedgerQueryApiTest.statistics` | - | - |

## Non-functional, database, accounting and business rules

| Requirement | Issue | PR | Verification | Skill | Brain note / ADR |
|---|---|---|---|---|---|
| NFR-PERF-01..03 latency, throughput, reconciliation time | #14 | M3 PR | `PerformanceBenchmarkTest` (manual; results in test-execution-report) | - | [[sqlite-write-lock-and-driver-behaviour]] |
| NFR-SAFE-01 atomicity | #9 | M2 PR | `RollbackFailureInjectionTest` (3 failure points), `TransferRollbackTest` | - | [[testing-concurrency-and-failure-injection]] |
| NFR-SAFE-02 consistency | #10 | M2 PR | reconciliation PASS at the end of API/concurrency tests | simulate-transfer step 5 | [[financial-reconciliation-and-auditability]] |
| NFR-SAFE-03 isolation | #14 | M3 PR | `ConcurrencyApiTest` | simulate-transfer step 4 | [[sqlite-concurrency-and-acid-transactions]] |
| NFR-SAFE-04 durability | #14 | M3 PR | `DurabilityTest` | - | [[testing-concurrency-and-failure-injection]], ADR-010 |
| NFR-SAFE-05 idempotency | #14 | M3 PR | `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` (10 concurrent + 10 sequential) | simulate-transfer step 3 | [[api-idempotency-and-retry-semantics]] |
| NFR-SAFE-06 immutability | #9 | M2 PR | `LedgerImmutabilityTest` | - | [[immutable-ledger-and-audit-trails]] |
| NFR-SEC-01 BCrypt >= 10 | #5 | M1 PR | `AuthApiTest.passwordsAreBcryptHashed` | - | [[api-security-and-input-validation]] |
| NFR-SEC-02 JWT HS256, >= 256-bit secret from env, 60 min | #5 | M1 PR | `JwtServiceTest` | - | ADR-006 |
| NFR-SEC-03 every endpoint x every role | #5 | M1 PR | `SecurityAccessMatrixTest` | - | [[api-error-contract-and-access-control]] |
| NFR-SEC-04 validation | #5 | M1 PR | `ErrorHandlingAndInputSafetyTest`, amount/key boundary tests | simulate-transfer E03-E05 | [[input-validation-and-boundary-checking]] |
| NFR-SEC-05 parameterised SQL | #5 | M1 PR | `ErrorHandlingAndInputSafetyTest.sqlInjectionIsNotExecuted` | - | [[api-security-and-input-validation]] |
| NFR-SEC-06 no internals in responses, no secrets in logs | #5 | M1 PR | `ErrorHandlingAndInputSafetyTest.noInternalsLeak`, `AuthApiTest.logsContainNoSecrets`, `RollbackFailureInjectionTest` | - | [[api-error-contract-and-access-control]] |
| NFR-REL-01 error format, rollback on unhandled errors | #5 | M1 PR | `ErrorHandlingAndInputSafetyTest.errorBodyHasTheStandardShape` | - | [[api-error-contract-and-access-control]] |
| NFR-REL-02 health reads the database | #5 | M1 PR | `HealthApiTest` | local-check (step 4) | [[sqlite-concurrency-and-acid-transactions]] |
| NFR-MAIN-01/02 modules acyclic, ledger sole writer | #5, #9 | M1 PR, M2 PR | `ArchitectureRulesTest` | - | ADR-001, ADR-003 |
| NFR-MAIN-03 persistence, clock, IDs behind interfaces | #5 | M1 PR | repository interfaces, `IdGenerator`, `Clock`; `JwtServiceTest` uses a fixed clock; spies replace repositories in rollback tests | - | ADR-001 |
| NFR-MAIN-04 OpenAPI with headers and error codes | #16 | M3 PR | `OpenApiDocumentationTest` | - | - |
| NFR-TEST-01..07 (TQ-1..TQ-6) | #12, #14 | all | see `docs/testing/test-plan.md`; coverage 96.5% overall | local-check (step 2) | [[testing-strategy-and-quality-gates]] |
| DB-01..04 SQLite file, PRAGMAs, integer/CHECK, uniqueness | #5 | M1 PR | `DatabaseConfigurationTest` | local-check | [[sqlite-concurrency-and-acid-transactions]], ADR-002 |
| DB-05 retention | #9 | M2 PR | immutability triggers; idempotency records kept >= 24 h (never deleted) | - | [[immutable-ledger-and-audit-trails]] |
| ACC-01..04 system accounts, derived balances, posting rules | #9 | M2 PR | `AdminDirectoryApiTest.listsAccountsIncludingSystemFunding`, `TestFundingApiTest.goldenRule`, `ReversalApiTest`, reconciliation C3 | simulate-transfer step 5 | [[double-entry-accounting-principles]] |
| BR-01..10 | #9, #13 | M2 PR, M3 PR | amount, currency, destination, status (`TransferApiTest.otherBusinessRules`, `deactivatedWalletsTakeNoPart`, `PaymentAndMerchantApiTest.paymentRejections`), reversal tests above | simulate-transfer E02-E13 | [[double-entry-accounting-principles]] |
| UI-01 rupee formatting, Indian grouping | Stage 4 A1 | A1, S1 | `frontend/src/lib/money.test.ts` (backend `MoneyFormatterTest` cases), screen tests | - | [[react-client-money-and-idempotency]], ADR-011 |
| UI-02 exact paise parsing, max two decimals | Stage 4 A1 | A1 | `money.test.ts` (incl. TQ-6 guard: no `parseFloat`/`toFixed`/`/ 100`), `TransferPage`/`TestFundingPage` tests | - | [[monetary-representation-and-integer-units]], ADR-004, ADR-011 |
| UI-03 role screens, every error code readable | Stage 4 A1 | A1, G1, S1 | `App.test.tsx`, per-feature navigation tests, `errorMessages.test.ts` (checks all codes of `ErrorCode.java`) | - | [[api-error-contract-and-access-control]], ADR-011 |
| UI-04 new key per submission, same key on retry | Stage 4 A1 | A1, G1, S1 | `useFinancialSubmit.test.ts`, retry tests in `UserPayments.test.tsx` and `AdminLedger.test.tsx`, `client-contract.int.test.ts` (real backend) | simulate-transfer step 3 | [[api-idempotency-and-retry-semantics]], ADR-005, ADR-011 |
| UI-05 submit disabled while in flight | Stage 4 A1 | A1, S1 | `useFinancialSubmit.test.ts` (double click), `UserPayments.test.tsx` | - | [[react-client-money-and-idempotency]] |
| SRS 3.1 account name in top bar (decision D3) | Stage 4 A1 | A1 | `AuthApiTest.loginReturnsDisplayNameForEveryRole`, `App.test.tsx` | - | ADR-011 |

## Skill requirements

| Requirement | Issue | PR | Verification | Skill | Brain note |
|---|---|---|---|---|---|
| SK-01.1..01.4 simulate-transfer | #15 | M3 PR | `skill-evaluations.md` (steps 1-6, E01-E15, S1-S3); `SkillDefinitionsTest` | simulate-transfer | [[testing-concurrency-and-failure-injection]] |
| SK-02.1/02.2 local-check | #6 | M1 PR | `skill-evaluations.md` (L1, L2); CI job `local-check` | local-check | [[sqlite-concurrency-and-acid-transactions]] |
| Brain skills (SDLC 1) | #11 | M2 PR | `BrainVaultTest`, `SkillDefinitionsTest`, `skill-evaluations.md` (B1-B4) | brain-ingest, brain-refine, brain-query | [[financial-reconciliation-and-auditability]] |

## SDLC requirements

| Requirement | Evidence |
|---|---|
| SB-1 links | `BrainVaultTest.noBrokenWikilinks` |
| SB-2 review notes | [[stage-1-review]], [[stage-2-review]], [[stage-3-review]] |
| SB-3 ADRs and skills cite distilled notes | `BrainVaultTest.everyAdrCitesADistilledNote`, `SkillDefinitionsTest.everySkillCitesADistilledNote` |
| SB-4 AGENTS.md | `AGENTS.md` |
| SB-5 brain changes via PRs | M2 PR (brain refine) |
| SB-6 every member authors notes | `authors` frontmatter + commit history of each member's PR |
| TQ-7 skill evaluations | `docs/testing/skill-evaluations.md` |
| TQ-8 regression tests per defect | `docs/testing/defect-log.md` |
