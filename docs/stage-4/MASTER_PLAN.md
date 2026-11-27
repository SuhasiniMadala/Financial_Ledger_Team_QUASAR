# Stage 4 Master Plan - Financial Ledger, Team QUASAR

Status: **PROPOSAL, waiting for team approval** (PR from `stage4/member-3-plan`). Nobody starts implementation until Akshaya and Geethika have both approved this PR.

Companion documents in this folder: `API_CONTRACTS.md` (backend as built + shared frontend contracts), `TASK_DEPENDENCIES.md` (PR order, file owners, conflicts), `ACCEPTANCE_CRITERIA.md` (requirement-to-test matrix), `DEMONSTRATION_CHECKLIST.md` (review-day script), and one instruction file per member: `INSTRUCTIONS_AKSHAYA.md`, `INSTRUCTIONS_GEETHIKA.md`, `INSTRUCTIONS_SUHASINI.md`.

---

## 1. Source documents and their status

| Document | Status | Action |
|---|---|---|
| SDLC "The AI-Assisted Software Development Life Cycle" | Read in full (supplied PDF) | Section 7 Stage 4 deliverables drive this plan |
| SRS **v1.4**, 12 Nov 2026 | **Not available**. The repository and every copy on Suhasini's laptop are **v1.2, 9 Nov 2026**. | This plan uses v1.2 plus the requirement text quoted in the team's Stage 4 brief. **Before A1 starts**, whoever has v1.4 adds it to the repository (replacing the v1.2 `.docx`) in a small PR, and Suhasini re-checks SRS 3.1, UI-01..UI-05 and Table 3-1 against this plan. |
| Project Statement (Financial Ledger & Digital Wallet API) | **Not available** (the "problem statement" files on the laptop belong to other courses). | Same: add it to the repo or share it; re-check this plan against it. |

Requirement IDs below (UI-xx, FR-xx, NFR-xx, SK-xx) are as numbered in SRS v1.2.

## 2. Repository audit (main @ `b1be847`, tag `v0.3.0-stage3`)

Every row was checked in code, Git or GitHub on 2026-11-27, not taken from the README.

| Area | Required by | Current status | Evidence | Stage 4 action |
|---|---|---|---|---|
| REST API, all of Table 3-1 | SRS 3.3 | COMPLETE | 27 operations in `module/*/controller/*.java`; `OpenApiDocumentationTest` checks all are documented | none - frontend consumes it |
| Registration / login / JWT | FR-AUTH-01..09, NFR-SEC-02 | COMPLETE | `AuthController`, `JwtService` (claims: `sub`, `role`, 60 min), `AuthApiTest`, `SecurityAccessMatrixTest` | none |
| Account name for the top bar | SRS 3.1 | PARTIALLY COMPLETE | `LoginResponse` = `{accessToken, tokenType, expiresInSeconds, accountId, role}`; no name in JWT | G-1: add `displayName` to `LoginResponse` (Akshaya, A1) |
| Roles and authorization | FR-AUTH-08/09, NFR-SEC-03 | COMPLETE | `SecurityConfig.java` lines 39-42; 404-not-403 for others' data | UI hides screens, backend stays authoritative |
| Error format + codes | SRS 3.4, Table 3-2, NFR-REL-01 | COMPLETE | `ErrorResponse.java`, `ErrorCode.java` (17 codes), `GlobalExceptionHandler` (`Retry-After: 1` on 503) | map all codes to messages (UI-03) |
| Paging + filters | SRS 3.4, FR-LED-11, FR-PAY-05 | COMPLETE | `PageQuery` (0-based, size 1-100), `PageResponse`, `DateRange` (yyyy-MM-dd, inclusive, UTC) | shared `usePagedQuery`, `Pagination`, `DateRangeFilter` |
| Idempotency | FR-TRF-02..08, FR-CON-08 | COMPLETE | `IdempotencyService`, key regex `^[A-Za-z0-9_-]{8,64}$`, `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` | UI-04 key handling in `useFinancialSubmit` |
| Integer money | TQ-6, BR-01, ADR-004 | COMPLETE | `...Minor` longs; `accept-float-as-int=false`; `MoneyFormatter` | UI-01/UI-02 in `money.ts` |
| CORS | (needed by a browser client on another port) | NOT IMPLEMENTED | `SecurityConfig` has no CORS | not needed: Vite proxy `/api` -> `:8080` (ADR-011) |
| React client, all role screens | SRS 3.1, UI-01..05, SDLC 7 Stage 4 | NOT IMPLEMENTED | no `frontend/`, no `package.json` in the repository | whole Stage 4 build (A1, A2, G1, G2, S2) |
| Frontend tooling | - | NOT IMPLEMENTED | none in repo; Suhasini's laptop has Node v24.18.0 / npm 11.16.0, Java 21.0.6, Maven 3.9.11 | Node 20+ required for everyone (check `node -v`) |
| CI | SDLC 4 | COMPLETE for the backend (7 jobs green on every PR); NOT IMPLEMENTED for a frontend | `.github/workflows/ci.yml` | S1: frontend lint/types/unit/build job + integration job against a real backend |
| Branch protection on `main` | `docs/branching-strategy.md` | NOT IMPLEMENTED | GitHub API: "Branch not protected" | Suhasini (repo owner) enables it: 1 approval + required CI checks |
| Skills | SDLC 3, SK-01, SK-02 | COMPLETE (Stage 3 runs) | `brain/skills/{simulate-transfer,local-check,brain-query,brain-ingest,brain-refine}` | run live in the review (D12-D14); re-record TQ-7 results |
| ADRs | SE-3, SB-3 | COMPLETE (ADR-001..010) | `docs/adr/` | ADR-011 frontend architecture (Akshaya, A1) |
| Traceability matrix | SE-5 | PARTIALLY COMPLETE | `docs/architecture/traceability-matrix.md`: UI-01..05 row says "Stage 4" | S3 fills UI rows with PRs and tests |
| User guide | SE-7, SRS 2.6 | DOCUMENTATION MISMATCH | `docs/user-guide.md` says "until the React client arrives (Stage 4), use Swagger/Postman" | S3 rewrites it for the React client |
| README | SE-7 | PARTIALLY COMPLETE | no frontend setup or run section | A1 adds "Frontend" section; S3 the rest |
| API docs | SE-7 | COMPLETE | `docs/api/openapi.json`, Postman collection | A1 refreshes both after G-1 |
| Stage 3 release | SDLC 4 | COMPLETE | tag `v0.3.0-stage3` -> `b1be847` on GitHub; CHANGELOG entry; CI on `main` green (attempt 3 after a Maven Central 429) | none |
| Stale Stage 3 items | SDLC 6 | DOCUMENTATION MISMATCH | Geethika's AI-session note says "BUILD SUCCESS - **N** tests"; Suhasini's has two unticked review lines | Geethika fixes hers in G1; Suhasini fills hers in this PR or S3 |
| Version number | release convention | NEEDS VERIFICATION | `pom.xml` `0.3.0-SNAPSHOT`; tags `v<major>.<minor>.<patch>-stage<N>`; SRS calls the product "Release 1.0" | recommend `v1.0.0-stage4` and pom `1.0.0`; team decides in S3 |

## 3. Stage 4 objective and required deliverables

SDLC 7, Stage 4 - Review: (1) live demonstration of the FRs **in the client**; (2) run `simulate-transfer` end-to-end; (3) run `local-check` live; (4) run `brain-query` live; (5) walk through one requirement from brain note -> ADR -> code -> test; (6) final release tag. Also: SDLC 4 (PRs with review, CI on every PR, release tags + changelog), SB-2 (Stage 4 review note), SB-6 (everyone authors notes, visible in history), SDLC 6 (everyone can explain merged code; AI sessions recorded), TQ-7 (skill evaluations before submission), TQ-8 (defects get regression tests).

SRS 3.1 client: top bar (product name, account name, Logout), role-specific navigation; USER Dashboard (wallets, balances, create wallet), Transfer, Pay Merchant, History & Detail; MERCHANT Account, Payments, Statistics; ADMIN Users & Merchants, Wallets (status + test funding), Transactions & Ledger (reversal), Reconciliation, Statistics; UI-01..UI-05.

## 4. Frontend architecture (decision recorded as ADR-011 by Akshaya)

- **Stack:** React + TypeScript + Vite in `frontend/`; `react-router-dom`; Vitest + React Testing Library + jsdom; ESLint. Plain CSS (global + CSS Modules). No UI kit, axios, uuid or money library, so as few dependencies as possible. TypeScript is the "equivalent of API types": every DTO in `API_CONTRACTS.md` A.4 becomes a TypeScript type in the owning feature's `types.ts`.
- **Talking to the backend:** relative URLs `/api/v1/...`; Vite `server.proxy` and `preview.proxy` forward `/api` to `http://localhost:8080`. So there's no CORS and no backend change. One `ApiClient` (`createApiClient`), bound to the session through `useApiClient()`; feature API modules are plain functions taking the client, so the same code runs in integration tests against the real backend.
- **Authentication:** login stores `{token, accountId, role, displayName, expiresAt}` in `sessionStorage` (survives a page refresh, gone when the tab closes). Any 401 or a passed `expiresAt` logs out and shows "session expired". Limitation (in ADR-011): any script running in the page could read `sessionStorage`. Mitigation: React escapes output, no `dangerouslySetInnerHTML`, no third-party scripts, 60-minute tokens. An httpOnly cookie would need backend changes, so it's out of scope.
- **Role-based UI:** a route registry per feature (`features/<x>/routes.tsx`) gives each role its nav and routes. `RequireRole` guards them; a wrong role sees a Forbidden page. The backend still checks every request.
- **Money:** `formatMinor` (UI-01) and `parseRupeesToPaise` (UI-02) in `src/lib/money.ts`, string and integer arithmetic only. A guard test fails the build if `parseFloat(`, `toFixed(` or `/ 100` appear in `frontend/src`.
- **Idempotency + double submit:** `useFinancialSubmit` creates a `crypto.randomUUID()` key per new logical submission. It reuses the same key and body on Retry after a network error, timeout or 503 (UI-04), and disables the button and ignores re-entry while a request is in flight (UI-05). Every financial form uses it: Transfer, Pay Merchant, Test funding, Reversal.
- **Errors:** `messageFor(ApiError)` maps all 17 codes (+ network, timeout) to sentences (UI-03); 422 responses show the FAILED transaction ID.
- **Truth comes from the backend:** a success message is only shown from the 201 `TransactionReceipt`; balances are re-fetched after every money action. The UI never computes a new balance itself.

## 5. Three-member allocation

Effort is in rough points (1 = small screen with tests). The proposal in the brief was adjusted: Suhasini takes USER History & Detail as well as the MERCHANT screens, so that Akshaya, who carries the critical-path foundation, has only three USER screens after it.

| Member | Feature work | Requirements | Effort |
|---|---|---|---|
| **Akshaya** (`Akula-Akshaya`) | A1: client foundation (scaffold, proxy, API client, auth, role routing, layout, shared components, money, error messages, `useFinancialSubmit`, test helpers), Login/Register pages, backend `displayName` (G-1), ADR-011. A2: USER Dashboard + create wallet, Transfer, Pay Merchant | UI-01..05 (shared code), UI-03, FR-AUTH-01/02/06/07, FR-WAL-01..06, FR-TRF-01..14 (client side), FR-PAY-01..03 | 8 + 4 = 12 |
| **Geethika** (`Geethika2611`) | G1: ADMIN Users & Merchants, Wallets (accounts list, freeze/unfreeze/deactivate with reason + confirmation, test funding). G2: Transactions & Ledger (filters, detail with entries, account entries, reversal), Reconciliation, Statistics | FR-ADM-01..07, FR-WAL-07/08, FR-LED-07..10, FR-REC-01..08, UI-04/05 on admin forms | 5 + 6 = 11 |
| **Suhasini** (`SuhasiniMadala`) | P0 this plan. S1: frontend CI jobs + integration-test harness. S2: USER History & Detail, MERCHANT Account, Payments, Statistics. S3: docs, traceability, test report, TQ-7 runs, Stage 4 review note, CHANGELOG, release prep | FR-LED-11/12, FR-PAY-04..07, FR-AUTH-09 (client side), SDLC 4 CI, SE-5, SE-7, SB-2, TQ-7 | 2 + 3 + 5 + 2 = 12 |

### 5.1 File ownership

| Member | Feature | Exact files/folders owned | Requirements | Tests owned | Documentation owned |
|---|---|---|---|---|---|
| Akshaya | Foundation + auth + USER money | `frontend/` root config files, `frontend/src/{main.tsx,App.tsx,api,auth,lib,hooks,components,pages,styles,test}/**`, `frontend/src/features/user-money/**`, backend `src/main/java/.../module/auth/dto/LoginResponse.java` + `AuthService`/`AuthController` lines for G-1, `.gitignore` | as above | `src/**/__tests__` in her folders, money guard test, `tests/integration/user-money.int.test.ts`, `AuthApiTest` update | ADR-011, `brain/resources/react-client-auth-money-and-idempotency.md`, README "Frontend" section, `AGENTS.md` frontend commands, `docs/api/openapi.json` + Postman refresh, her AI-session note |
| Geethika | ADMIN console | `frontend/src/features/admin/**` | as above | `features/admin/__tests__`, `tests/integration/admin.int.test.ts` | `brain/areas/admin-console-operations.md`, her AI-session note, fix of her Stage 3 note |
| Suhasini | CI, harness, USER history, MERCHANT, docs/release | `.github/workflows/ci.yml`, `frontend/vitest.integration.config.ts`, `frontend/tests/integration/{setup.ts,helpers.ts,smoke.int.test.ts}`, `frontend/src/features/user-history/**`, `frontend/src/features/merchant/**`, `docs/stage-4/**`, `docs/user-guide.md`, `docs/testing/**`, traceability matrix, `CHANGELOG.md`, `pom.xml` version | as above | `features/{user-history,merchant}/__tests__`, `tests/integration/{history,merchant}.int.test.ts` | `brain/projects/stage-4-client-and-review.md`, `brain/resources/client-integration-testing-against-real-backend.md`, `brain/projects/stage-4-review.md`, her AI-session note, README (non-frontend sections) |

`brain/index.md` and `brain/log.md`: everyone appends only their own lines (see `TASK_DEPENDENCIES.md` section 5).

### 5.2 API integration owners (all 27 operations)

| Owner | Operations |
|---|---|
| Akshaya | `POST /auth/register`, `POST /auth/register/merchant`, `POST /auth/login`, `POST /wallets`, `GET /wallets`, `GET /wallets/{id}`, `POST /transfers` ★, `POST /payments` ★ |
| Suhasini | `GET /health` (harness readiness), `GET /transactions`, `GET /transactions/{id}`, `GET /merchant/account`, `GET /merchant/transactions`, `GET /merchant/statistics` |
| Geethika | `GET /admin/users`, `GET /admin/merchants`, `GET /admin/accounts`, `POST /admin/wallets/{id}/freeze`, `/unfreeze`, `/deactivate`, `POST /admin/wallets/{id}/test-funding` ★, `GET /admin/transactions`, `GET /admin/transactions/{id}`, `POST /admin/transactions/{id}/reversal` ★, `GET /admin/accounts/{id}/entries`, `GET /admin/statistics`, `POST /admin/reconciliations` |

## 6. Sequence

See `TASK_DEPENDENCIES.md`. In short: **P0 -> A1 -> (S1, A2, G1, S2 in parallel) -> G2 -> full test-matrix run + demo rehearsal -> S3 -> release tag (with team approval)**. Reviewers rotate: Akshaya's PRs are reviewed by Suhasini or Geethika, Geethika's by Suhasini or Akshaya, Suhasini's by Geethika or Akshaya. Nobody approves their own PR. Merge with "Create a merge commit".

## 7. Testing strategy

| Level | Tool | Runs | Owner |
|---|---|---|---|
| Unit (money, error map, idempotency hook, session) | Vitest | `npm test`, CI | Akshaya |
| Component (every screen: loading / empty / error / success; role guard; disabled button) | Vitest + React Testing Library with `fakeClient` | `npm test`, CI | screen owner |
| Integration (feature API modules against the **real** backend on a fresh SQLite file; checks stored balances; ends with reconciliation PASS) | Vitest `--config vitest.integration.config.ts` | `npm run test:integration` locally (backend running) and in CI job "Frontend integration" | harness: Suhasini; tests: each feature owner |
| Manual UI (browser, evidence screenshots) | `ACCEPTANCE_CRITERIA.md` M rows, `DEMONSTRATION_CHECKLIST.md` | step 5 | all three |
| Backend regression | existing `mvn -B verify` (153 tests) | CI | unchanged; Akshaya extends `AuthApiTest` for G-1 |

Mocks are allowed **only** in unit/component tests. Integration evidence must come from the real backend (no mocked responses).

## 8. CI requirements (S1, Suhasini)

Add to `.github/workflows/ci.yml` (existing 7 jobs unchanged):
1. **Frontend (lint, types, unit tests, build)**: `actions/setup-node@v4` (Node 20, npm cache on `frontend/package-lock.json`), `npm ci`, `npm run lint`, `npm run typecheck`, `npm test -- --run`, `npm run build`.
2. **Frontend integration (real backend)**: build the jar, generate a random `LEDGER_JWT_SECRET` and admin password **inside the job** (no repository secrets), start the jar on a temp SQLite file, wait for `/api/v1/health`, run `npm run test:integration`, always stop the server.

Known risk: Maven Central occasionally answers 429 (seen on 2026-11-27); re-run the job, never weaken it.

## 9. Brain and documentation (SB-1..6, SE-3, SE-5, SE-7)

| Item | Owner | PR |
|---|---|---|
| `brain/projects/stage-4-client-and-review.md` (project note, links this plan) | Suhasini | P0 |
| ADR-011 Frontend architecture, citing `[[react-client-auth-money-and-idempotency]]` | Akshaya | A1 |
| `brain/resources/react-client-auth-money-and-idempotency.md` | Akshaya | A1 |
| `brain/resources/client-integration-testing-against-real-backend.md` | Suhasini | S1 |
| `brain/areas/admin-console-operations.md` (safe admin actions: reasons, confirmations, idempotent funding/reversal, reading a reconciliation report) | Geethika | G1 |
| AI-session note per member for their Stage 4 work (goal, what worked/failed, human review checklist) | each | their last PR |
| `brain/projects/stage-4-review.md` (SB-2) | Suhasini drafts, all comment in the PR | S3 |
| README, user guide, traceability, test execution report, skill evaluations, CHANGELOG | Suhasini | S3 |

## 10. Release process

1. All PRs merged after review; CI green on `main`.
2. Full test matrix (`ACCEPTANCE_CRITERIA.md`) run with evidence; demo rehearsed.
3. S3 merged: CHANGELOG moves `[Unreleased]` to the release entry, version in `pom.xml` updated.
4. **The team explicitly approves the tag** (a comment "approved for release" by Akshaya and Geethika on the S3 PR).
5. Suhasini creates the annotated tag on the S3 merge commit and pushes it. Proposed name: `v1.0.0-stage4` (format `v<major>.<minor>.<patch>-stage<N>` from `docs/branching-strategy.md`; 1.0.0 because SRS 1.1 defines the product as "Release 1.0").

## 11. Final acceptance checklist

- [ ] SRS v1.4 and Project Statement added to the repo and checked against this plan
- [ ] Branch protection on `main` enabled (1 approval, required checks)
- [ ] A1, S1, A2, G1, S2, G2, S3 merged with independent reviews and green CI
- [ ] Every row of `ACCEPTANCE_CRITERIA.md` PASS with evidence (or an agreed limitation)
- [ ] D01-D16 rehearsed with evidence
- [ ] TQ-7: `simulate-transfer`, `local-check`, `brain-query` re-run and recorded
- [ ] Docs match the product (README, user guide, traceability, test report, CHANGELOG, Stage 4 review note)
- [ ] Each member's commits and PRs visible under their own account
- [ ] Release tag created after explicit team approval

## 12. Plan validation (Phase D)

| Check | Result |
|---|---|
| Every SRS UI requirement assigned | UI-01, UI-02, UI-04, UI-05: Akshaya (shared code) + every financial form owner uses it; UI-03: Akshaya (guards, messages) + each screen owner shows them |
| Every required screen assigned | USER 4 (A: Dashboard, Transfer, Pay; S: History & Detail); MERCHANT 3 (S); ADMIN 5 (G); plus Login/Register (A) |
| Every API integration has an owner | yes, section 5.2 (27 operations) |
| Every shared file has one owner | yes, `TASK_DEPENDENCIES.md` section 3 |
| Every task has acceptance criteria | yes, `ACCEPTANCE_CRITERIA.md` + per-PR checks |
| Independent commit ownership | yes, one branch per PR, only the owner commits to it |
| Stage 4 SDLC deliverables covered | yes, D01-D16 cover (1)-(6) |
| Open items | SRS v1.4 and Project Statement not yet seen; G-1 needs team approval; version name needs team approval |
