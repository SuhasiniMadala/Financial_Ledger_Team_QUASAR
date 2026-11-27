# Stage 4 Live Demonstration Checklist (SDLC section 7, Stage 4 - Review)

Rehearse this once, completely, before the review. Save evidence under `docs/stage-4/evidence/` with names like `D07-merchant-payments.png`. Before committing, check every screenshot: **no password, token, JWT or secret may be visible**. Use the `.test` demo emails below, never real ones.

## 0. Roles and who presents

| Part | Steps | Presenter |
|---|---|---|
| Setup, login/registration, USER money screens, UI-01..UI-05 | D01-D06, D10, D11 | Akshaya |
| USER history, MERCHANT screens, role restrictions | D07, D08, D10 (merchant part) | Suhasini |
| ADMIN screens | D09 | Geethika |
| `simulate-transfer` end-to-end | D12 | Suhasini |
| `local-check` live | D13 | Akshaya |
| `brain-query` live | D14 | Geethika |
| Requirement walkthrough (brain note -> ADR -> code -> test) | D15 | Suhasini |
| Release tag + green CI | D16 | Suhasini |

## 1. Prepare (once, before the review)

Run in **terminal 1**, from the repository root (Windows PowerShell):

```powershell
git switch main
git pull origin main
mvn -B -DskipTests package
$env:LEDGER_JWT_SECRET = -join ((48..57 + 65..90 + 97..122) | Get-Random -Count 48 | ForEach-Object { [char]$_ })
$env:LEDGER_ADMIN_EMAIL = 'admin@demo.test'
$env:LEDGER_ADMIN_PASSWORD = Read-Host 'Choose a demo admin password (min 8 chars, not shown on screen-share)'
java -jar (Get-ChildItem target/financial-ledger-*.jar | Select-Object -First 1).FullName --ledger.db.path=target/demo.db
```

`target/demo.db` is a fresh, throw-away database, so the demo never touches anyone's dev data. `target/` is git-ignored. Check: `GET http://localhost:8080/api/v1/health` -> `{"status":"UP","database":"UP"}`.

**Terminal 2:**

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173` in Chrome, Firefox or Edge.

Demo accounts (create them live in D02, or beforehand in a rehearsal of the same database):

| Role | Display / business name | Email |
|---|---|---|
| USER | Asha Rao | `asha@demo.test` |
| USER | Ravi Kumar | `ravi@demo.test` |
| MERCHANT | Campus Cafe | `cafe@demo.test` |
| ADMIN | Platform Admin | value of `LEDGER_ADMIN_EMAIL` |

## 2. Script

| # | Step | What to show | Expected | Evidence |
|---|---|---|---|---|
| D01 | Backend + frontend running | health JSON; login page | `UP/UP`; login page loads | D01-health.png |
| D02 | Register | Asha and Ravi as USER, Campus Cafe as MERCHANT | success; merchant code `MER-xxxxxx` shown | D02-register-merchant.png |
| D03 | USER login + Dashboard | log in as Asha; top bar; create INR wallet; copy wallet ID. Log in as Ravi and create his INR wallet too | top bar: product name, "Asha Rao", Logout; wallet `₹0.00` ACTIVE | D03-dashboard.png |
| D04 | Admin test funding | log in as admin; Wallets; fund Asha Rs 5,000.00 and Ravi Rs 2,000.00 | receipts; balances `₹5,000.00` / `₹2,000.00` | D04-funding.png |
| D05 | Transfer | Asha -> Ravi Rs 1,000.00 "rent share" | receipt `T-...`; Asha `₹4,000.00`, Ravi `₹3,000.00` | D05-transfer.png |
| D06 | Pay merchant | Asha pays Campus Cafe Rs 250.00 "Order 1187" | receipt; Asha `₹3,750.00` | D06-payment.png |
| D07 | History & detail | Asha's history (filters); open the transfer; Ravi sees it as INCOMING | own entry only, counterparty name | D07-history-detail.png |
| D08 | MERCHANT | Campus Cafe: Account, Payments (date filter), Statistics | Rs 250.00 from "Asha Rao", reference; no payer wallet ID; 1 payment, total = average = `₹250.00` | D08-merchant.png |
| D09 | ADMIN | Users & Merchants; Wallets (freeze Ravi -> Asha's transfer fails WALLET_NOT_ACTIVE -> unfreeze); Transactions & Ledger (entries of the transfer); reverse it ("sent to wrong recipient"); reverse again -> ALREADY_REVERSED; Reconciliation -> PASS; Statistics | as described; reconciliation PASS | D09-*.png |
| D10 | Role restrictions + errors | as Campus Cafe open `/user/transfer`; as Asha open `/admin/wallets`; insufficient funds message | Forbidden page; readable messages (UI-03) | D10-roles.png |
| D11 | UI-01..UI-05 | amounts `₹1,00,000.00` style; type `10.505` -> rejected; DevTools Network: Idempotency-Key UUID; stop the backend, submit, restart, press Retry -> same key, one transaction; button disabled while sending | as described | D11-*.png |
| D12 | simulate-transfer | `powershell -ExecutionPolicy Bypass -File brain/skills/simulate-transfer/scripts/simulate-transfer.ps1 -Evaluate -ReportPath target/simulate-transfer-report.md` (needs the backend from section 1; see its SKILL.md "Before you run it") | every step PASS, E01-E14 PASS | target report, copied into skill-evaluations.md |
| D13 | local-check | stop the demo backend first (port 8080), then `powershell -ExecutionPolicy Bypass -File brain/skills/local-check/scripts/local-check.ps1` | 5 steps PASS; port free afterwards | report output |
| D14 | brain-query | ask (via the brain-query skill): "Why does a retried payment never charge twice, and which ADR decided it?" | answer cites `[[api-idempotency-and-retry-semantics]]` and ADR-005 | answer text |
| D15 | Walkthrough FR-CON-08 / NFR-SAFE-05 | `brain/resources/api-idempotency-and-retry-semantics.md` -> `docs/adr/ADR-005-idempotency-key-pattern.md` -> `IdempotencyService.java` + `TransferService.java` -> `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` -> UI side `useFinancialSubmit.ts` (UI-04) and its test | each link opened in the IDE; traceability-matrix row shown | - |
| D16 | Release + CI | GitHub: tag list, last `main` CI run green, CHANGELOG entry | final tag present; all jobs green | D16-ci.png |

## 3. Recovery if a step fails

| Problem | Fix |
|---|---|
| Port 8080 or 5173 already in use | `Get-NetTCPConnection -LocalPort 8080 -State Listen \| Select-Object OwningProcess`, then `Stop-Process -Id <id>`; restart |
| Every request suddenly 401 | the backend was restarted with a new `LEDGER_JWT_SECRET`, so old tokens are invalid. Log in again. |
| Admin login fails | `LEDGER_ADMIN_EMAIL` / `LEDGER_ADMIN_PASSWORD` were not set in terminal 1 **before** the first start on a fresh DB. Stop, delete `target/demo.db*`, set them, start again |
| Maven build fails with "429 Too Many Requests" | Maven Central rate limit (seen on 2026-11-27). Wait a few minutes and retry; build the jar before the review |
| `npm ci` fails | delete `frontend/node_modules`, run `npm ci` again; Node 20+ required (`node -v`) |
| Data got messy during rehearsal | stop the backend, delete `target/demo.db*`, restart, redo D02-D04 |
| Reconciliation shows FAIL | do not hide it: open the discrepancy table, explain it, and record it as a defect (TQ-8) |
