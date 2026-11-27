# Stage 4 Acceptance Criteria and Requirement-to-Test Matrix

**Rule:** a row may say PASS only after it was actually run, with the evidence column filled in: a test name and CI run link, or a screenshot path under `docs/stage-4/evidence/`. Nothing below has been run yet. A page that renders is **not** a pass: money tests check stored balances (admin Wallets screen or API) and finish with reconciliation PASS (AGENTS.md testing rules).

How a row is tested:
- **U**: unit or component test (Vitest + React Testing Library, fake client), runs in CI.
- **I**: integration test against the **real** backend (`npm run test:integration`), runs locally and in CI.
- **M**: manual run through the browser, recorded with a screenshot.

## 1. Definition of done (whole stage)

1. Every row below is PASS with evidence, or listed as a known limitation in the Stage 4 review note and agreed by the team.
2. All PRs in `TASK_DEPENDENCIES.md` are merged after an independent review, with CI green on each PR and on `main`.
3. The SDLC 7 Stage 4 deliverables in `DEMONSTRATION_CHECKLIST.md` are rehearsed once end-to-end with evidence.
4. Docs describe the real product: README, user guide, traceability matrix, test execution report, skill evaluations, CHANGELOG, Stage 4 review note.
5. Each member's commits appear under their own GitHub account (section 4).

## 2. Requirement-to-test matrix

| Test ID | Requirement | Role | Scenario | Expected result | Owner | How | Actual result | Evidence |
|---|---|---|---|---|---|---|---|---|
| UI-01-a | UI-01 | all | `formatMinor` for 0, 1, 99, 100, 100000, 10000000, 1234567, INR and USD | `₹0.00`, `₹0.01`, `₹0.99`, `₹1.00`, `₹1,000.00`, `₹1,00,000.00`, `₹12,345.67`, `$12,345.67`; same as backend `MoneyFormatterTest` | Akshaya | U | not run | |
| UI-01-b | UI-01 | all | every amount on every screen uses `<Money>` / `formatMinor` | no raw paise or `toFixed` output visible | screen owner | M + review | not run | |
| UI-02-a | UI-02, TQ-6 | USER, ADMIN | `parseRupeesToPaise`: `1`->100, `1.5`->150, `1.50`->150, `1,00,000.00`->10000000, `0.01`->1 | exact integers | Akshaya | U | not run | |
| UI-02-b | UI-02 | USER, ADMIN | rejects `1.505`, `-1`, `1e3`, `abc`, ``, `0`, `0.00` | error shown, **no request sent** | Akshaya | U | not run | |
| UI-02-c | TQ-6 | - | no-float guard: a test scans `frontend/src/**` for `parseFloat(`, `toFixed(` and `/ 100` | none found outside comments | Akshaya | U | not run | |
| UI-03-a | UI-03 | each role | navigation shows only that role's screens | USER: Dashboard, Transfer, Pay Merchant, History. MERCHANT: Account, Payments, Statistics. ADMIN: Users & Merchants, Wallets, Transactions & Ledger, Reconciliation, Statistics | Akshaya | U | not run | |
| UI-03-b | UI-03, FR-AUTH-08 | USER | open `/admin/wallets` by URL | Forbidden page; no admin data fetched | Akshaya | U + M | not run | |
| UI-03-c | UI-03 | all | `messageFor` returns a readable sentence for all 17 codes + network + timeout | no raw code shown alone | Akshaya | U | not run | |
| UI-04-a | UI-04 | USER | new transfer submission | new UUID in `Idempotency-Key` | Akshaya | U | not run | |
| UI-04-b | UI-04 | USER | first attempt fails with network error, timeout or 503, user clicks Retry | same key and same body resent; backend returns one transaction | Akshaya | U + I | not run | |
| UI-04-c | UI-04 | USER | user edits a field after an error, then submits | a **new** key | Akshaya | U | not run | |
| UI-04-d | UI-04, FR-TRF-05 | USER | integration: send the same transfer twice with one key | 2nd response has `Idempotent-Replayed: true`, same `transactionId`, balance debited once | Akshaya | I | not run | |
| UI-05-a | UI-05 | USER, ADMIN | button state while request in flight | disabled; double click sends exactly one request | Akshaya (hook), Geethika (admin forms) | U | not run | |
| AUTH-1 | FR-AUTH-01/02 | visitor | register USER and MERCHANT through the UI | success message; merchant sees merchant code | Akshaya | U + M | not run | |
| AUTH-2 | FR-AUTH-06, G-1 | all | login | top bar shows product name, display name, Logout | Akshaya | U + M | not run | |
| AUTH-3 | FR-AUTH-07 | all | token expired or any 401 | session cleared, redirected to login with "session expired" | Akshaya | U | not run | |
| AUTH-4 | FR-AUTH-03 | visitor | register with an existing email | readable DUPLICATE_RESOURCE message | Akshaya | U | not run | |
| USR-1 | FR-WAL-01..06 | USER | create INR wallet; create USD wallet; try a 2nd INR wallet | two wallets listed with ID, balance `₹0.00`, ACTIVE; 3rd attempt shows DUPLICATE_RESOURCE | Akshaya | I + M | not run | |
| USR-2 | FR-TRF-01, 14 | USER | transfer Rs 1,000.00 to another USER wallet | receipt with `T-...`; both balances change by exactly 100000 paise; reconciliation PASS | Akshaya | I + M | not run | |
| USR-3 | FR-TRF-11, 13 | USER | transfer more than the balance | INSUFFICIENT_FUNDS message with FAILED transaction ID; balance unchanged | Akshaya | I + M | not run | |
| USR-4 | FR-TRF-12 | USER | transfer to own wallet / USD wallet / merchant wallet / frozen wallet | SAME_WALLET / CURRENCY_MISMATCH / INVALID_DESTINATION / WALLET_NOT_ACTIVE messages | Akshaya | I | not run | |
| USR-5 | FR-PAY-01..03 | USER | pay `MER-...` Rs 250.00 with reference | receipt; merchant balance +25000 paise; reconciliation PASS | Akshaya | I + M | not run | |
| USR-6 | FR-PAY-02 | USER | pay unknown merchant code | INVALID_DESTINATION message | Akshaya | I | not run | |
| USR-7 | FR-LED-11 | USER | history shows outgoing, incoming and own FAILED rows, newest first; filters wallet/type/status/date; paging | correct rows and counts | Suhasini | I + M | not run | |
| USR-8 | FR-LED-12 | USER | open a transaction detail | type, status, amount, time, description, counterparty name, failure code, **own** entries only, reversal link | Suhasini | I + M | not run | |
| USR-9 | FR-AUTH-09 | USER | open `/user/history/<another user's T-id>` | "not found" message, no data | Suhasini | I | not run | |
| MER-1 | FR-PAY-04 | MERCHANT | account screen | business name, merchant code, wallet ID, balance, status | Suhasini | I + M | not run | |
| MER-2 | FR-PAY-05/06 | MERCHANT | payments list with date filter | time, amount, payer name, reference; reversals shown; **no payer wallet ID or balance anywhere in the page** | Suhasini | I + M | not run | |
| MER-3 | FR-PAY-07 | MERCHANT | statistics default (30 days) and custom range | count, total, average (half-even) match the backend | Suhasini | I + M | not run | |
| MER-4 | FR-AUTH-08 | MERCHANT | no Transfer/Pay screens; direct URL `/user/transfer` | Forbidden page; backend would answer 403 | Suhasini | U + M | not run | |
| ADM-1 | FR-ADM-01 | ADMIN | users and merchants lists, paged | rows with name, email, registered time, merchant code | Geethika | I + M | not run | |
| ADM-2 | FR-ADM-02 | ADMIN | accounts list incl. `SYS-FUND-INR` | balance and status per account | Geethika | I + M | not run | |
| ADM-3 | FR-WAL-07, UI-04/05 | ADMIN | test-fund a USER wallet Rs 5,000.00 | receipt; balance +500000; button disabled in flight; retry reuses key | Geethika | I + M | not run | |
| ADM-4 | FR-ADM-04/05 | ADMIN | freeze -> unfreeze -> deactivate with reasons; invalid transition | status updates; INVALID_STATE_TRANSITION message; deactivate asks for confirmation | Geethika | I + M | not run | |
| ADM-5 | FR-ADM-03 | ADMIN | transactions list with filters; detail with all entries; account entries page | entries balance (debits = credits) | Geethika | I + M | not run | |
| ADM-6 | FR-LED-07..09 | ADMIN | reverse a COMPLETED transfer with a 10+ character reason | REVERSAL receipt; original shows "reversed by"; balances restored; reconciliation PASS | Geethika | I + M | not run | |
| ADM-7 | FR-LED-09 | ADMIN | reverse it again; reverse a FAILED one | ALREADY_REVERSED / NOT_REVERSIBLE messages | Geethika | I | not run | |
| ADM-8 | FR-REC-01..08 | ADMIN | run reconciliation | PASS/FAIL, per-currency totals, counts, discrepancy table (empty on PASS) | Geethika | I + M | not run | |
| ADM-9 | FR-ADM-07 | ADMIN | statistics screen | users, merchants, wallets by status, transactions by status, completed totals, latest reconciliation | Geethika | I + M | not run | |
| SEC-1 | NFR-SEC-02/06 | - | no secrets, tokens or admin credentials in frontend code, docs or screenshots | gitleaks green; review | everyone | CI + review | not run | |
| SEC-2 | UI-03, privacy | all | logout then press Back | no cached private data shown; login required | Akshaya | M | not run | |
| INT-1 | SK-01 | - | `simulate-transfer -Evaluate` live | all steps PASS, E01-E14 PASS | Suhasini | skill run | not run | |
| INT-2 | SK-02 | - | `local-check` live | 5 steps PASS | Akshaya | skill run | not run | |
| INT-3 | SDLC 1 | - | `brain-query` live on a real question | answer cites brain notes | Geethika | skill run | not run | |

## 3. Per-PR acceptance (the reviewer checks these)

- CI green on the PR (backend jobs + frontend jobs once S1 is merged).
- New screens have component tests for loading, empty, error and success states.
- Financial forms use `useFinancialSubmit` and `parseRupeesToPaise`, never their own key or amount logic.
- The PR touches only files owned by its author (see `TASK_DEPENDENCIES.md` section 3).
- PR description lists: purpose, requirements, files, tests run with results, docs updated, known limitations.
- The reviewer ran the screen against a local backend and says so in the review.

## 4. Attribution check (done once before the release)

```powershell
git fetch origin
git log origin/main --no-merges --since=2026-11-28 --format='%h %an <%ae> %s'
gh pr list --state merged --search "stage4" --json number,author,title --jq '.[] | "#\(.number) \(.author.login) \(.title)"'
```

Each Stage 4 PR's author and commits must be the person named in `TASK_DEPENDENCIES.md`.
