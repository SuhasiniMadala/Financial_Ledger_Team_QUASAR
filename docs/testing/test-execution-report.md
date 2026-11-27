# Stage 3 Test Execution Report

Owner: Suhasini M (issue #17). Every number below comes from a real run on 2026-11-27. Nothing is estimated.

## 1. Environment

| Item | Value |
|---|---|
| Machine | Windows 11 Pro (10.0.26300), developer laptop (not the SRS 2.4 reference machine) |
| Java / Maven | Oracle JDK 21.0.6 (code compiled for Java 17) / Maven 3.9.11 |
| Database | SQLite 3 via sqlite-jdbc 3.45.3.0, WAL, `synchronous=FULL`, a fresh file per test context |
| Shell for skills | Windows PowerShell 5.1.26100 |
| Code | branch `stage3/member-3-transfers-history` on top of `main` @ `3c5c491` (after PRs #23 and #24 were merged), re-run after the PR #25 review fixes |

## 2. Full automated suite - `mvn -B clean verify`

Result: **BUILD SUCCESS - 153 tests, 0 failures, 0 errors, 0 skipped.** Checkstyle: 0 violations. JaCoCo: "All coverage checks have been met." (Re-run on 2026-11-27 after adding `TransferApiTest.deactivatedWalletsTakeNoPart` and `PaymentAndMerchantApiTest.statisticsCountReversedPaymentsAsCompleted`; the first run had 151 tests.)

| Test class | Tests | Failed | Errors | Skipped | Time (s) |
|---|---|---|---|---|---|
| ArchitectureInvariantsTest (Stage 1 placeholder) | 8 | 0 | 0 | 0 | 0.05 |
| SkeletonSanityTest (Stage 1 placeholder) | 1 | 0 | 0 | 0 | 0.00 |
| architecture.ArchitectureRulesTest | 3 | 0 | 0 | 0 | 0.47 |
| common.id.IdGeneratorTest | 3 | 0 | 0 | 0 | 0.24 |
| common.util.MoneyFormatterTest | 11 | 0 | 0 | 0 | 0.24 |
| config.DatabaseConfigurationTest | 4 | 0 | 0 | 0 | 8.10 |
| docs.OpenApiDocumentationTest | 1 | 0 | 0 | 0 | 1.05 |
| module.admin.AdminDirectoryApiTest | 7 | 0 | 0 | 0 | 1.76 |
| module.admin.AdminLedgerQueryApiTest | 3 | 0 | 0 | 0 | 0.99 |
| module.admin.ReversalApiTest | 5 | 0 | 0 | 0 | 1.99 |
| module.admin.RollbackFailureInjectionTest | 3 | 0 | 0 | 0 | 2.93 |
| module.admin.TestFundingApiTest | 8 | 0 | 0 | 0 | 2.28 |
| module.auth.AuthApiTest | 11 | 0 | 0 | 0 | 1.63 |
| module.auth.service.JwtServiceTest | 4 | 0 | 0 | 0 | 0.03 |
| module.health.HealthApiTest | 2 | 0 | 0 | 0 | 0.34 |
| module.idempotency.IdempotencyRulesTest | 10 | 0 | 0 | 0 | 0.04 |
| module.ledger.LedgerImmutabilityTest | 3 | 0 | 0 | 0 | 0.66 |
| module.ledger.LedgerServiceTest | 5 | 0 | 0 | 0 | 1.27 |
| module.reconciliation.ReconciliationApiTest | 3 | 0 | 0 | 0 | 1.03 |
| module.reconciliation.ReconciliationDetectsCorruptionTest | 2 | 0 | 0 | 0 | 1.66 |
| module.transfer.BusyTimeoutTest | 1 | 0 | 0 | 0 | 1.71 |
| module.transfer.ConcurrencyApiTest | 4 | 0 | 0 | 0 | 3.56 |
| module.transfer.DurabilityTest | 1 | 0 | 0 | 0 | 12.17 |
| module.transfer.PaymentAndMerchantApiTest | 7 | 0 | 0 | 0 | 3.54 |
| module.transfer.service.MerchantAverageTest | 8 | 0 | 0 | 0 | 0.02 |
| module.transfer.TransactionHistoryApiTest | 4 | 0 | 0 | 0 | 2.31 |
| module.transfer.TransferApiTest | 12 | 0 | 0 | 0 | 6.44 |
| module.transfer.TransferRollbackTest | 1 | 0 | 0 | 0 | 1.24 |
| module.wallet.WalletApiTest | 6 | 0 | 0 | 0 | 1.73 |
| repository.BrainVaultTest | 4 | 0 | 0 | 0 | 0.34 |
| repository.SkillDefinitionsTest | 3 | 0 | 0 | 0 | 0.02 |
| security.ErrorHandlingAndInputSafetyTest | 4 | 0 | 0 | 0 | 0.55 |
| security.SecurityAccessMatrixTest | 1 | 0 | 0 | 0 | 0.57 |
| **Total** | **153** | **0** | **0** | **0** | |

The two Stage 1 placeholder classes test logic written inside the test itself, not application code; they were kept unchanged (lint fix only) and are not counted as evidence for any requirement.

## 3. Coverage (JaCoCo, line coverage) - NFR-TEST-07

| Package | Lines covered | Coverage |
|---|---|---|
| module.ledger | 188 / 191 | 98.4% |
| module.transfer | 174 / 174 | 100.0% |
| module.idempotency | 69 / 73 | 94.5% |
| module.reconciliation | 67 / 69 | 97.1% |
| module.admin | 153 / 153 | 100.0% |
| module.auth | 146 / 159 | 91.8% |
| module.wallet | 133 / 141 | 94.3% |
| module.health | 9 / 9 | 100.0% |
| config | 141 / 147 | 95.9% |
| common.* | 170 / 177 | 96.0% |
| `FinancialLedgerApplication` (main method) | 1 / 3 | 33.3% |
| **All production code** | **1251 / 1296** | **96.5%** |

NFR-TEST-07 requires >= 70% for ledger, transfer, idempotency and reconciliation: **PASS** (enforced by the build).

## 4. Performance - NFR-PERF-01..03

Command: `mvn test -Dtest.excludedGroups=none -Dgroups=perf -Djacoco.skip=true -Dcheckstyle.skip=true -Dperf.sustainSeconds=300`. Result: **PASS** (1 test, 357 s).

| Measure | Target | Result |
|---|---|---|
| Ledger entries before load | >= 1,00,000 | 100,040 (seeded in 17.8 s) |
| p95 latency, transfers, 10 concurrent clients, 1,000 requests | <= 1000 ms | **65 ms** |
| p95 latency, reads (`GET /transactions`), 10 clients, 1,000 requests | <= 500 ms | **276 ms** |
| Sustained committed transfers per second over 300 s | >= 10 | **477.9** (143,371 committed of 143,384 requests) |
| HTTP 503 rate during sustained load | < 1% | **0.009%** (13) |
| Reconciliation after load | PASS | **PASS** |
| Reconciliation time over 388,782 entries | <= 30 s | **6.0 s** |

Deviation: the SRS names JMeter; we used an equivalent Java load generator with 10 concurrent HTTP clients (`PerformanceBenchmarkTest`). The laptop is not the SRS reference machine. The test is excluded from CI because it takes about 6 minutes.

## 5. Requirement scenarios from the SRS (selected evidence)

| SRS scenario | Test | Result |
|---|---|---|
| 4.3.2 #1-#6 transfers | `TransferApiTest` | PASS |
| 4.3.2 #7 database failure while writing the credit entry | `TransferRollbackTest` | PASS |
| 4.5.2 #1 two Rs 400 from Rs 500 | `ConcurrencyApiTest.twoConcurrentOverdraftsOnlyOneWins` | PASS (exactly one 201) |
| 4.5.2 #3 same request and key at the same instant | `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` | PASS (one transaction, identical bodies) |
| 4.5.2 #4 lock held longer than busy timeout | `BusyTimeoutTest` | PASS (503, no effect, retry 201) |
| 4.6.2 #2 DEBIT 100000 / CREDIT 99999 | `LedgerServiceTest.unbalancedPostingIsRejectedAndNothingIsWritten` | PASS |
| 4.6.2 #3 direct SQL UPDATE/DELETE | `LedgerImmutabilityTest` | PASS (rejected by triggers) |
| 4.7.2 #2 edited balance Rs 2,000 -> Rs 2,500 | `ReconciliationDetectsCorruptionTest` | PASS (expected 200000, actual 250000, +50000) |
| 4.7.2 #3 transfers during reconciliation | `ReconciliationApiTest.snapshotIsConsistentUnderConcurrentWrites` | PASS |
| NFR-SAFE-03 100 concurrent Rs 10 debits on Rs 500 | `ConcurrencyApiTest.hundredConcurrentDebits` | PASS |
| NFR-SAFE-04 kill after 201, restart | `DurabilityTest` | PASS |

## 6. Skills (TQ-7)

See `skill-evaluations.md`: local-check PASS on Windows; simulate-transfer PASS (steps 1-6) with evaluation cases E01-E14 PASS and E15 NOT RUN (covered by `TransferRollbackTest`); brain validators PASS.

## 7. Checks not run locally

| Check | Status | Reason / where it runs |
|---|---|---|
| gitleaks secret scan | **NOT RUN locally** | Downloading and running the external gitleaks binary was not permitted in the working session. It runs in CI on every PR (job "Secret scanning"). A manual search found only synthetic, low-entropy test values. |
| CI on GitHub | **NOT RUN yet** | Requires the three Stage 3 branches to be pushed by their owners. Results are to be added to `docs/ci/ci-pipeline.md` with the run links. |
| React client UI-01..UI-05 | **NOT IN SCOPE** | Planned for Stage 4 (development plan). |

## 8. Next action

Each member pushes their slice and opens the PR; CI results and PR links are added here and in the traceability matrix when available.
