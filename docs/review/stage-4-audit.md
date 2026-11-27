# Stage 4 Audit - Financial Ledger, Team QUASAR

Date: 2026-11-28. Auditor: Suhasini M, with an AI assistant (Claude Code); human review required before merge (SDLC 6).
Baseline: `main` @ `b1be847` (tag `v0.3.0-stage3`). Sources of truth: SRS v1.2 (9 Nov 2026, `Financial_Wallet_Ledger_SRS (2) (1).docx`) and the SDLC document (section 7, Stage 4).

Every row below was checked in the code, in Git or on GitHub on the audit date. Nothing is taken from earlier reports without re-checking.

## 1. Repository state

| Item | Observed | Evidence |
|---|---|---|
| Local branch | `stage4/member-3-plan`, 2 commits ahead of `main` (`858526a` Stage 4 plan docs, `46abea0` brain Stage 4 note) | `git log --oneline -3` |
| Working tree | 8 files under `docs/stage-4/` deleted but not committed | `git status` |
| Remote | `origin` = github.com/SuhasiniMadala/Financial_Ledger_Team_QUASAR | `git remote -v` |
| Tags | `v0.3.0-stage3` only; convention `v<major>.<minor>.<patch>-stage<N>` (`docs/branching-strategy.md` line 34) | `git tag --list` |
| GitHub Releases | none (the Stage 3 tag has no Release page) | `gh release list` |
| Branch protection on `main` | not enabled at audit time; **enabled on 2026-11-28 after decision D5** | `gh api .../branches/main/protection` returned 404 "Branch not protected" before, and the rule set after |
| Issues | #5-#22 all CLOSED; no open issues | `gh issue list --state all` |
| Pull requests | #1-#4, #23-#29 all MERGED; none open | `gh pr list --state all` |
| CI on `main` | last 10 runs `success` (latest run 46044841337, merge of PR #29) | `gh run list` |
| Toolchain (Suhasini's laptop) | Java 21.0.6, Maven 3.9.11, Node v24.18.0, npm 11.16.0 | `java -version`, `mvn -v`, `node -v` |
| Fresh build | `mvn -B verify`: **Tests run: 153, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS** (Checkstyle + JaCoCo gate included) | run on 2026-11-28 on `stage4/member-3-plan` (no `src/` changes vs `main`) |

## 2. SRS v1.2 compliance

| SRS area | Requirements | Status | Evidence |
|---|---|---|---|
| REST API (Table 3-1) | all 27 operations | COMPLETE | `module/*/controller/*.java`; `OpenApiDocumentationTest`; `SecurityAccessMatrixTest` |
| Communications (3.4) | headers, paging 20/100, error shape, codes of Table 3-2 | COMPLETE | `PageQuery`, `ErrorResponse`, `ErrorCode`, `GlobalExceptionHandler`, `IdempotentResponse.REPLAYED_HEADER` |
| Registration & auth (4.1) | FR-AUTH-01..09 | COMPLETE | `AuthApiTest`, `JwtServiceTest`, `SecurityAccessMatrixTest`; DTO limits (`@Size(min=8,max=72)`) |
| Wallets (4.2) | FR-WAL-01..08 | COMPLETE | `WalletApiTest`, `TestFundingApiTest` |
| Transfers (4.3) | FR-TRF-01..14 | COMPLETE | `TransferApiTest`, `IdempotencyRulesTest`, `TransferRollbackTest` |
| Merchant payments (4.4) | FR-PAY-01..07 | COMPLETE | `PaymentAndMerchantApiTest`, `MerchantAverageTest` (30-day default in `MerchantService`) |
| Concurrency (4.5) | FR-CON-01..08 | COMPLETE | `ConcurrencyApiTest`, `BusyTimeoutTest`, `RollbackFailureInjectionTest` |
| Ledger & audit (4.6) | FR-LED-01..12 | COMPLETE | `LedgerServiceTest`, `LedgerImmutabilityTest`, `ReversalApiTest`, `TransactionHistoryApiTest` |
| Reconciliation (4.7) | FR-REC-01..08 | COMPLETE | `ReconciliationApiTest`, `ReconciliationDetectsCorruptionTest` |
| Administration (4.8) | FR-ADM-01..07 | COMPLETE | `AdminDirectoryApiTest`, `AdminLedgerQueryApiTest` |
| NFR 5.1-5.4, BR, DB, ACC | all | COMPLETE (NFR-PERF measured manually, results in `docs/testing/test-execution-report.md`) | `docs/architecture/traceability-matrix.md` |
| Skills (6.3) | SK-01.1..01.4, SK-02.1..02.2 | COMPLETE (Stage 3 evaluations) - must be **re-run** for Stage 4 (TQ-7) | `brain/skills/*`, `docs/testing/skill-evaluations.md` |
| **User interface (3.1)** | top bar, role navigation, 12 role screens | **NOT IMPLEMENTED** | no `frontend/`, no `package.json` anywhere in the repository |
| **UI-01..UI-05** | rupee formatting, exact paise parsing, role screens + error messages, idempotency key reuse, disabled submit | **NOT IMPLEMENTED** | traceability matrix row "UI-01..UI-05 React client - Stage 4" |
| Account name in top bar (3.1) | client needs the logged-in name | **GAP** | `LoginResponse` = `{accessToken, tokenType, expiresInSeconds, accountId, role}`; the JWT has no name and Table 3-1 has no "me" endpoint |
| User documentation (2.6) | README, OpenAPI + Swagger UI, per-role guide, Postman incl. negative/idempotency cases | PARTIAL | all present, but `docs/user-guide.md` line 3 says "Until the React client arrives (Stage 4), use Swagger UI" |
| Operating environment (2.4) | "React single-page app in current Chrome, Firefox or Edge" | NOT IMPLEMENTED | as above |

**Conclusion:** the backend fully satisfies SRS v1.2 and is green. The only unmet SRS requirements are the React client (3.1, UI-01..UI-05, 2.4) and the account name it needs. The SDLC also requires the Stage 4 FR demonstration to be "in client". Swagger UI and Postman are API tools, not the client the SRS specifies.

## 3. Defect triage

| Defect | Issue | State | Verified fix | Stage 4 relevance |
|---|---|---|---|---|
| DEF-001 test config shadowed main config | #18 | Closed | `DatabaseConfigurationTest.testOverridesDoNotShadowMainConfiguration` passes (2026-11-28 build) | none |
| DEF-002 BusyTimeoutTest locked wrong file | #19 | Closed | `BusyTimeoutTest` passes | none |
| DEF-003 OpenAPI dangling ref / duplicate header / 200 vs 201 | #20 | Closed | `OpenApiDocumentationTest` passes | refresh `docs/api/openapi.json` if `LoginResponse` changes |
| DEF-004/005 local-check crash on PS 5.1, server left running | #21 | Closed | fix in `local-check.ps1`; must be **re-proven** by the live Stage 4 run | blocker if it regresses (live demo) |
| DEF-006 simulate-transfer input column / exit code | #22 | Closed | must be **re-proven** by the Stage 4 `-Evaluate` run | blocker if it regresses (live demo) |

No open defects. No issue was found "fixed but not closed" or "closed but not fixed".

## 4. Documentation mismatches found

| File | Problem | Owner (proposed) |
|---|---|---|
| `brain/projects/ai-sessions/2026-11-27-geethika-ledger-and-reconciliation.md` line 36 | "BUILD SUCCESS - **N** tests" placeholder never filled | Geethika |
| `brain/projects/ai-sessions/2026-11-27-suhasini-transfers-and-history.md` lines 32-33 | two unticked human-review lines | Suhasini |
| `brain/projects/stage-4-client-and-review.md` | links to `docs/stage-4/MASTER_PLAN.md`, which is deleted in the working tree | Suhasini (depends on decision D2) |
| `docs/user-guide.md` | says the React client "arrives in Stage 4" | Suhasini, after the client is merged |
| `docs/architecture/traceability-matrix.md` | UI-01..05 row empty | Suhasini, after the client is merged |
| `README.md` | run command hard-codes `financial-ledger-0.3.0-SNAPSHOT.jar`; no frontend section | Akshaya (frontend), Suhasini (version) |

## 5. Stage 4 deliverables (SDLC section 7)

| Deliverable | Status |
|---|---|
| 1. Live demonstration of FRs in the client | NOT POSSIBLE YET - no client |
| 2. `simulate-transfer` end-to-end | skill present; Stage 4 run not done |
| 3. `local-check` live | skill present; Stage 4 run not done |
| 4. `brain-query` live | skill present; Stage 4 run not done |
| 5. Walk-through brain note -> requirement -> issue -> ADR -> code -> test -> PR | candidate chain exists (FR-CON-08, see section 7); not documented |
| 6. Final release tag | not done; no GitHub Release exists for any tag |
| SB-2 Stage 4 review note, TQ-7 results, CHANGELOG entry, AI-session notes | not done |

## 6. Risks for the final demonstration

| Risk | Impact | Mitigation |
|---|---|---|
| React client is the largest remaining piece of work and is on the critical path | demo item 1 fails | small shared foundation first (one PR), then screens in parallel by area |
| Maven Central returned HTTP 429 once in Stage 3 CI | red CI on review day | re-run the job; never weaken it |
| No branch protection on `main` | an unreviewed push breaks the release | owner enables it: 1 approval + required CI checks |
| Live skills depend on local ports 8080/18080 and env vars | live demo stalls | `docs/review/demo-environment.md` with reset/restart steps |
| Client fakes or recomputes money | violates TQ-6 / BR-01 | client shows only backend values; paise parsing by string; guard test |

## 7. Candidate traceability chain (to be verified and written up in Phase 6)

FR-CON-08 / FR-TRF-05 (idempotent retry executes once): `[[api-idempotency-and-retry-semantics]]` (brain/resources) -> SRS FR-TRF-04..08, FR-CON-08 -> issues #9 and #14 -> ADR-005 (+ ADR-009 for the lock) -> `module/idempotency/service/IdempotencyService.java` -> `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce`, `TransferApiTest` replay test -> PR #24 / #25. After the client is merged, it extends to UI-04 in the client's idempotency hook.

## 8. Team decisions (approved 2026-11-28)

| ID | Decision | Effect |
|---|---|---|
| D1 | Build the React client required by SRS 3.1 and UI-01..UI-05 | Stage 4 implementation work, split by area (section 9) |
| D2 | The earlier plan files in `docs/stage-4/` stay deleted; `docs/review/` replaces them | deletion committed in this PR; the brain project note now links here |
| D3 | Add `displayName` to the login response (one field, no new endpoint) | top-bar account name (SRS 3.1); regression test `AuthApiTest.loginReturnsDisplayNameForEveryRole` |
| D4 | Final tag follows `v<major>.<minor>.<patch>-stage<N>`: **`v1.0.0-stage4`** | created only after every Stage 4 check passes on `main` |
| D5 | Branch protection on `main` | **enabled 2026-11-28**: 1 approving review, the 7 CI checks required, applies to admins, no force pushes or deletion |
| D6 | Work split accepted by all three members | section 9 |

## 9. Work split

| Member | PR (branch) | Owns |
|---|---|---|
| Akshaya A | A1 `stage4/member-1-auth-wallet-review` | backend `displayName`; client foundation (API client, session, role routing, top bar, money UI-01/02, error messages UI-03, idempotent submit UI-04/05); Login, Register, USER Dashboard, ADMIN Users & Merchants, ADMIN Wallets (status changes); ADR-011; local-check demonstration |
| Geethika M | G1 `stage4/member-2-ledger-review` | ADMIN test funding, Transactions & Ledger (entries, reversal), account entries, Reconciliation, Statistics; demo environment; brain-query demonstration; traceability walkthrough |
| Suhasini M | P0 `stage4/member-3-plan` (this audit), S1 `stage4/member-3-transfer-review`, R1 `stage4/member-3-release` | USER Transfer, Pay Merchant, History & Detail; MERCHANT Account, Payments, Statistics; frontend CI jobs and real-backend integration test; simulate-transfer evaluation; demo script; Stage 4 review, CHANGELOG and release |

Merge order: P0 -> A1 -> G1 -> S1 -> R1. Each PR is reviewed by another member.

## 10. Readiness assessment

**Backend, tests, CI and skills: ready.** **Stage 4 as a whole: NOT READY** at audit time. The SRS-required React client did not exist, none of the live Stage 4 runs had been recorded, and no release had been prepared. Decisions and the work split (sections 8 and 9) were approved by the team on 2026-11-28; progress is tracked in the Stage 4 review (`docs/review/stage-4-review.md`, written in R1).
