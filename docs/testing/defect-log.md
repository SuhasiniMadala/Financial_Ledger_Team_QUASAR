# Defect Log (TQ-8)

Every significant defect has a regression test or regression evaluation. Defects were found during Stage 3 development of this codebase; none was invented to fill the log. Section B lists defects found in the discarded `stage3-handoff` prototype, which the new tests now guard against.

## A. Defects in this codebase

| ID | Issue | Description | Severity | Steps to reproduce | Expected | Actual | Root cause | Fix | Regression test | Status |
|---|---|---|---|---|---|---|---|---|---|---|
| DEF-001 | #18 | Test configuration shadowed the main configuration | High | Put overrides in `src/test/resources/application.properties`; run any Spring test | Main properties + test overrides | Schema never created ("no such table: user_account"); strict JSON settings absent | Two files with the same classpath name; only one is loaded | Overrides moved to `src/test/resources/config/application.properties` | `DatabaseConfigurationTest.testOverridesDoNotShadowMainConfiguration` | Fixed |
| DEF-002 | #19 | `BusyTimeoutTest` locked the wrong database file and passed for the wrong reason | Medium | Read `${ledger.db.path}` (contains `${random.uuid}`) in a test, lock that file, send a transfer | 503 SERVICE_BUSY | 201 (another file was locked) | Spring's random property source returns a new value on each lookup | Test asks SQLite for the real file (`PRAGMA database_list`) | `BusyTimeoutTest` (asserts the path, then 503) | Fixed |
| DEF-003 | #20 | OpenAPI document wrong: dangling `ErrorResponse` ref, `Idempotency-Key` listed twice, 200 instead of 201 | Medium | `GET /v3/api-docs` | Valid refs, header once, 201 | 3 problems | springdoc rebuilds components after the `OpenAPI` bean; header declared twice; `ResponseEntity<String>` hides the status | Components and responses set in the `OpenApiCustomizer` | `OpenApiDocumentationTest` | Fixed |
| DEF-004 | #21 | local-check crashed while writing its report on Windows PowerShell 5.1 | Medium | Run local-check in PowerShell 5.1 | Report printed | Terminating `NativeCommandError` from `java -version` | stderr output + `ErrorActionPreference=Stop` | Native calls run with `Continue` | local-check evaluation run (skill-evaluations.md, L1) | Fixed |
| DEF-005 | #21 | local-check left the server running after it finished (violates SK-02.2) | High | Run local-check on a machine whose `java` is Oracle's `javapath` stub | Server stopped | Port 18080 still served by the real `java.exe` | The stub starts the JVM as another process; only the stub was stopped | Start the JVM from `java.home`; stop any process mentioning the run's temp folder; verify the port is free | local-check evaluations L1 and L2 check that no server is left | Fixed |
| DEF-006 | #22 | simulate-transfer: Input column empty; refusals exited 1 instead of 2 | Low | Run with `-Evaluate`; run with a non-local URL | Inputs listed; exit 2 | Empty column; exit 1 | `$input` is a reserved PowerShell variable; `Write-Error` aborted before `exit 2` | Renamed parameter; print + `exit 2` | simulate-transfer evaluations S1, S2, S3 | Fixed |
| DEF-007 | #33 | Frontend CI job ran the client tests on a Node.js version the test tools do not support | Medium | Run the `Frontend (lint, types, tests, build)` job with `node-version: '20'` | 12 test files, 111 tests pass (as on a developer laptop with Node 24) | `[vitest-pool]: Failed to start forks worker` for all 12 test files (`Cannot read properties of undefined (reading 'bind')`); 0 tests ran | Vitest 5 needs Node 22.12+ or 24+, and jsdom 31 needs Node 22.22.2+ or 24.15+; the CI jobs, `package.json` engines and the docs still said Node 20 | Both frontend CI jobs use Node 24; `engines` set to `^22.22.2 \|\| >=24.15.0`; README, ADR-011 and demo-environment updated | The `Frontend (lint, types, tests, build)` CI job itself (it fails when the tests cannot run) | Fixed |

Risk avoided rather than fixed (no defect occurred): sqlite-jdbc's `commit()` immediately starts the next transaction, so a connection-wide IMMEDIATE mode would hold the write lock while idle. Found by inspecting the driver before using it; design recorded in ADR-009.

Test mistakes corrected during development (no product defect): `AdminDirectoryApiTest` first expected 4 status-change rows where the scenario produces 3.

## B. Defects found in the discarded `stage3-handoff` prototype (guarded by new tests)

The prototype was audited on 2026-11-27, then deleted from GitHub at the team's request (backup bundle kept locally). These problems were reproduced with probe tests against it; the rewrite was tested for each:

| Prototype defect | Severity | Guarded by |
|---|---|---|
| 10 concurrent requests with one Idempotency-Key executed 4 transfers (user charged 4x) | Critical | `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` |
| Transfers to a merchant wallet or the System Funding account were accepted (BR-04) | High | `TransferApiTest.invalidDestinations` |
| Rejected transfers left no FAILED record and 422s were not replayed (FR-TRF-08/13) | High | `TransferApiTest.insufficientFunds`, `otherBusinessRules` |
| Someone else's wallet gave 403 instead of 404, revealing it exists (FR-AUTH-09) | Medium | `TransferApiTest.sourceMustBeOwned`, `WalletApiTest.otherUsersWalletIsNotFound` |
| Idempotency-Key characters not validated (FR-TRF-03) | Medium | `IdempotencyRulesTest.malformedKeys` |
| History showed only transactions the user started, never incoming ones (FR-LED-11) | Medium | `TransactionHistoryApiTest.recipientSeesIncomingTransfer` |
| No `GET /api/v1/health` (NFR-REL-02) | Medium | `HealthApiTest` |
