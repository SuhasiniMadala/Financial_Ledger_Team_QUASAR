# Financial Ledger - Team QUASAR

A double-entry digital wallet and ledger REST API (Java 17, Spring Boot 3, SQLite). Users hold wallets, transfer money to each other and pay merchants; an admin adds simulated money and can reverse transactions. Every operation is idempotent, safe under concurrency, append-only, and verifiable by reconciliation.

> Academic project. All money is simulated; there is no connection to any bank or payment network.

| | |
|---|---|
| Requirements | `Financial_Wallet_Ledger_SRS (2) (1).docx` (SRS v1.2) |
| Design | `docs/architecture/`, decisions in `docs/adr/` |
| API reference | Swagger UI at `/swagger-ui.html` when running; spec in `docs/api/openapi.json` |
| How each role uses the API | `docs/user-guide.md` |
| Tests and results | `docs/testing/` |
| Knowledge base | `brain/` (Obsidian vault) - start at `brain/index.md` |
| Team process | `AGENTS.md`, `docs/branching-strategy.md` |

## 1. Prerequisites

- Java 17 or newer (`java -version`)
- Maven 3.8 or newer (`mvn -v`)
- Node.js 20 or newer (`node -v`), for the React client only
- Git
- Windows PowerShell 5.1 (built into Windows) or PowerShell 7, for the skills

## 2. Get the code

```powershell
cd $HOME\Documents
git clone https://github.com/SuhasiniMadala/Financial_Ledger_Team_QUASAR.git
cd Financial_Ledger_Team_QUASAR
```

## 3. Build and test

```powershell
mvn -B verify
```

This runs Checkstyle (lint), all tests on fresh temporary databases, and the coverage rule. Expected end: `Tests run: ..., Failures: 0, Errors: 0` and `BUILD SUCCESS`. A one-command check of everything is the `local-check` skill:

```powershell
powershell -ExecutionPolicy Bypass -File brain/skills/local-check/scripts/local-check.ps1
```

## 4. Configuration

Secrets are never stored in the repository. Set them as environment variables in the terminal that starts the server:

| Variable / property | Required | Meaning |
|---|---|---|
| `LEDGER_JWT_SECRET` | yes (production) | Random string of at least 32 characters used to sign tokens (NFR-SEC-02). |
| `LEDGER_ADMIN_EMAIL`, `LEDGER_ADMIN_PASSWORD` | to use admin endpoints | The only way to create an ADMIN account (FR-AUTH-05). |
| `ledger.db.path` | no (`financial_ledger.db`) | SQLite file (DB-01). |
| `ledger.currencies` | no (`INR,USD`) | Supported wallet currencies (ADR-008). |
| `ledger.max-amount-minor` | no (`10000000`) | Per-transaction limit in paise (Rs 1,00,000.00, ADR-008). |
| `ledger.db.busy-timeout-ms` | no (`5000`) | Wait for the write lock before HTTP 503 (FR-CON-06). |

Properties can be given on the command line as `--name=value`.

## 5. Run

```powershell
mvn -B -DskipTests package
$env:LEDGER_JWT_SECRET = -join ((48..57 + 65..90 + 97..122) | Get-Random -Count 48 | ForEach-Object { [char]$_ })
$env:LEDGER_ADMIN_EMAIL = 'admin@local.test'
$env:LEDGER_ADMIN_PASSWORD = '<choose-a-local-password>'
java -jar (Get-Item target/financial-ledger-*.jar).FullName
```

Then open http://localhost:8080/swagger-ui.html or call `GET http://localhost:8080/api/v1/health` (expected `{"status":"UP","database":"UP"}`).

## 5a. Run the React client (SRS 3.1)

Needs Node.js 20 or newer (`node -v`). Keep the backend from section 5 running on port 8080, then in a **second** terminal:

```powershell
cd frontend
npm ci            # first time only: installs the exact versions in package-lock.json
npm run dev       # http://localhost:5173
```

Open http://localhost:5173, register a user or merchant, or log in with the admin from `LEDGER_ADMIN_EMAIL`. The dev server forwards every `/api` call to `http://localhost:8080` (set `LEDGER_API_URL` to use another backend). Checks: `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`. Design: ADR-011.

## 6. API summary (SRS Table 3-1, all under `/api/v1`, star = needs `Idempotency-Key`)

| Role | Endpoints |
|---|---|
| Public | `POST /auth/register`, `POST /auth/register/merchant`, `POST /auth/login`, `GET /health` |
| USER | `POST/GET /wallets`, `GET /wallets/{id}`, `POST /transfers` (star), `POST /payments` (star), `GET /transactions`, `GET /transactions/{id}` |
| MERCHANT | `GET /merchant/account`, `GET /merchant/transactions`, `GET /merchant/statistics` |
| ADMIN | `GET /admin/users`, `/admin/merchants`, `/admin/accounts`, `/admin/accounts/{id}/entries`, `/admin/transactions`, `/admin/transactions/{id}`, `/admin/statistics`; `POST /admin/wallets/{id}/freeze`, `/unfreeze`, `/deactivate`, `/test-funding` (star); `POST /admin/transactions/{id}/reversal` (star); `POST /admin/reconciliations` |

Money is always an integer number of minor units (`amountMinor`, paise for INR). Errors always look like `{"code","message","timestamp","path","requestId"}` with the codes of SRS Table 3-2.

## 7. Agent Skills (`brain/skills/`)

| Skill | What it does |
|---|---|
| `local-check` | Build, test, start, `GET /health`, report PASS/FAIL (SK-02) |
| `simulate-transfer` | Transfers, idempotent replay, concurrent overdraft attempt, reconciliation; `-Evaluate` runs TQ-7 cases (SK-01) |
| `brain-ingest`, `brain-refine`, `brain-query` | Maintain and query the second brain |

## 8. Project structure

```
src/main/java/com/quasar/ledger/
  common/   errors, IDs, money formatting, request IDs, roles
  config/   database + BEGIN IMMEDIATE transaction manager, security, OpenAPI, properties
  module/   auth, wallet, ledger (sole writer), idempotency, transfer, reconciliation, admin, health
src/main/resources/schema.sql   tables, constraints, immutability triggers
src/test/java/...               unit, API, concurrency, durability, validator and performance tests
frontend/                       React client (SRS 3.1): src/features/<area>/ screens, src/lib money + errors
brain/                          second brain + Agent Skills
docs/                           architecture, ADRs, API, testing, CI
```

## 9. Team

| Member | Stage 3 area |
|---|---|
| Akshaya A [BT2024215] | Accounts, wallets, admin directory, platform, security, local-check, lint |
| Geethika M [BT2024139] | Ledger engine, idempotency, funding, reversals, reconciliation, brain |
| Suhasini M [BT2024043] | Transfers, payments, merchant views, history, concurrency, simulate-transfer, CI |
