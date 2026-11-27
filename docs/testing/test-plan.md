# Stage 3 Test Strategy and Test Plan

Owner: Geethika M (issue #12). Covers SRS TQ-1..TQ-8 (SDLC 5) and NFR-TEST-01..07. Results: `test-execution-report.md`. Defects: `defect-log.md`. Skill evaluations: `skill-evaluations.md`.

## 1. Principles

1. Tests are written from SRS requirement IDs and the stimulus/response scenarios in SRS section 4; each test names the requirement or scenario in its `@DisplayName`.
2. **Money is verified in storage**, never only through HTTP status codes: tests read `account.balance_minor` and `ledger_entry`, and every end-to-end, concurrency and failure test ends with a reconciliation PASS (NFR-SAFE-02).
3. **Isolation (TQ-5):** each Spring test context uses a new SQLite file under `target/test-db/`; all data is synthetic (ADR-010). No real accounts, no production system.
4. **No fabricated results:** a test that cannot run is reported as NOT RUN with the reason. Failing tests are never deleted or weakened to get green.
5. **Integer money (TQ-6):** no `float`/`double` in production code (enforced by `ArchitectureRulesTest`).

## 2. Test levels

| Level | Tool | What | Examples |
|---|---|---|---|
| Unit | JUnit 5, AssertJ | pure rules and calculations (TQ-1, NFR-TEST-01) | `MoneyFormatterTest`, `IdempotencyRulesTest`, `MerchantAverageTest`, `JwtServiceTest`, `IdGeneratorTest` |
| Integration | Spring Boot test + real SQLite file | services + database, constraints, triggers, transactions (TQ-2) | `LedgerServiceTest`, `LedgerImmutabilityTest`, `DatabaseConfigurationTest`, `RollbackFailureInjectionTest` |
| API / end-to-end | embedded server on a random port, Java `HttpClient` | every Table 3-1 endpoint over real HTTP (TQ-3, NFR-TEST-03) | `TransferApiTest`, `PaymentAndMerchantApiTest`, `WalletApiTest`, `AuthApiTest`, ... |
| Security | API tests | roles, ownership, tokens, injection, error leakage (NFR-SEC-*) | `SecurityAccessMatrixTest`, `ErrorHandlingAndInputSafetyTest` |
| Concurrency | many threads released by a latch | races, overdraft, duplicate keys, busy lock (TQ-4) | `ConcurrencyApiTest`, `BusyTimeoutTest`, `ReconciliationApiTest` |
| System | separate JVM process | durability after a forced kill (NFR-SAFE-04) | `DurabilityTest` |
| Performance | Java load generator, tag `perf` (manual) | NFR-PERF-01..03 | `PerformanceBenchmarkTest` |
| Static / repository | JUnit over source files | architecture rules, SKILL.md format, brain schema and links | `ArchitectureRulesTest`, `SkillDefinitionsTest`, `BrainVaultTest` |
| Skills | PowerShell scripts | local-check, simulate-transfer (TQ-7) | `skill-evaluations.md` |

## 3. Negative and boundary data

| Field | Boundary / negative values tested |
|---|---|
| `amountMinor` | -50000, -1, 0, **1**, **10000000 (limit)**, 10000001, 100.5, "100", true, null, missing |
| Idempotency-Key | missing, 7 chars, **8**, **64**, 65, spaces, `/`, `;`, non-ASCII |
| password | 7, **8**, **72** chars, 73 chars, 72 chars but > 72 bytes |
| description / reference | **140** / **64** chars accepted, 141 / 65 rejected |
| reversal reason | 9 rejected, **10** accepted, 501 rejected |
| IDs | malformed (`bad`, `W-123`, SQL text) -> 400; well-formed but unknown -> 404 or 422 as specified |
| paging | size 0, **1**, **100**, 101, page -1, non-numeric |
| dates | wrong format, `to` before `from` |

## 4. Financial correctness checks

| Property | Test |
|---|---|
| Funding/transfer/payment move exactly the amount; entries balance-after correct | `LedgerServiceTest`, `TransferApiTest.successfulTransfer`, `TestFundingApiTest.fundAndReplay` |
| Rejected operations do not change balances and leave a FAILED record | `TransferApiTest.insufficientFunds`, `otherBusinessRules`, `invalidDestinations` |
| Failures leave no partial update | `RollbackFailureInjectionTest`, `TransferRollbackTest` |
| Duplicate requests charge once | `TransferApiTest.replay`, `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` |
| Concurrent requests behave as some serial order; never negative | `ConcurrencyApiTest` |
| Golden rule: System Funding = sum of wallets per currency | `TestFundingApiTest.goldenRule`, reconciliation C1+C3 |
| Half-even average rounding | `MerchantAverageTest` |
| Reconciliation detects corruption and changes nothing | `ReconciliationDetectsCorruptionTest`, `ReconciliationApiTest.reconciliationIsReadOnly` |

## 5. Requirement -> test map

The full Requirement -> Issue -> PR -> Test -> Skill -> Brain note map is `docs/architecture/traceability-matrix.md`.

## 6. How to run

| What | Command |
|---|---|
| Everything a PR needs | `mvn -B verify` |
| One class | `mvn test -Dtest=ConcurrencyApiTest -Djacoco.skip=true` |
| Performance (about 6 min) | `mvn test -Dtest.excludedGroups=none -Dgroups=perf -Djacoco.skip=true -Dcheckstyle.skip=true` |
| Coverage report | open `target/site/jacoco/index.html` after `mvn verify` |
| Skills | see `skill-evaluations.md` |

## 7. Entry and exit criteria for Stage 3

Entry: Stage 2 design accepted. Exit: all tests pass, coverage rule met, no open High defect, skill evaluations recorded, CI green on every merged PR, test execution report written with real numbers.

## 8. Known limitations

- Performance is measured with a Java load generator instead of JMeter (equivalent measurements; documented deviation) on one developer laptop, not the SRS reference machine.
- The React client (UI-01..UI-05) is out of Stage 3 scope (Stage 4 per the development plan); its requirements are not tested yet.

Related knowledge: [[testing-strategy-and-quality-gates]], [[testing-concurrency-and-failure-injection]].
