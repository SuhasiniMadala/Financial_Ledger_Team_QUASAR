---
name: simulate-transfer
description: Demonstrate and verify the Financial Ledger's core promise on a local test instance - create two synthetic wallets, fund one, transfer with an idempotency key and replay it, fire simultaneous transfers that exceed the balance, and confirm reconciliation PASS with a table of initial, final and expected balances. Optional -Evaluate runs the TQ-7 evaluation cases. Use before a stage submission and live in the Stage 4 review.
compatibility: Requires a running local Financial Ledger server, Windows PowerShell 5.1 or PowerShell 7, and the admin credentials the server was started with in LEDGER_ADMIN_EMAIL / LEDGER_ADMIN_PASSWORD.
---

# simulate-transfer (SRS SK-01)

## Purpose

Show, with real API calls, that money is never created, lost or duplicated: retries do not double-charge, simultaneous transfers never overdraw, and the ledger reconciles afterwards (FR-1 to FR-5). It is the product skill required by SDLC Stage 3 and run end-to-end in Stage 4.

## Inputs (SK-01.2)

| Input | Default | Meaning |
|---|---|---|
| `-BaseUrl` | `http://localhost:8080` | Local server. Any non-localhost host is refused. |
| `-AmountMinor` | 10000 (Rs 100.00) | Amount per transfer, in paise |
| `-Concurrency` | 10 | Number of simultaneous transfers in step 4 |
| `-Evaluate` | off | Also run the TQ-7 evaluation cases E01-E14 |
| `-ReportPath` | none | Save the Markdown report to a file |
| `LEDGER_ADMIN_EMAIL`, `LEDGER_ADMIN_PASSWORD` | - | Environment variables; never typed into the script or committed |

## Before you run it

1. Build the jar once: `mvn -B -DskipTests package`.
2. In terminal 1 start a **throw-away** local server (PowerShell). Choose your own admin password; it lives only in this terminal session:

   ```powershell
   $env:LEDGER_ADMIN_EMAIL = 'admin@local.test'
   $env:LEDGER_ADMIN_PASSWORD = '<choose-a-local-password>'
   java -jar (Get-Item target/financial-ledger-*.jar).FullName --ledger.db.path=target/simulate.db --ledger.jwt.allow-ephemeral-secret=true
   ```

3. In terminal 2 set the same two environment variables, then run:

   ```powershell
   powershell -ExecutionPolicy Bypass -File brain/skills/simulate-transfer/scripts/simulate-transfer.ps1 -Evaluate -ReportPath target/simulate-transfer-report.md
   ```

## Steps performed (SK-01.1, in order, public API only)

| Step | Action | Pass condition |
|---|---|---|
| 1 | Register two synthetic users, create INR wallets A and B, admin funds A with `AmountMinor x Concurrency` | Both ACTIVE; funding HTTP 201 |
| 2 | Read initial balances | A = funded amount, B = 0 |
| 3 | Transfer A -> B with a new key, then resend identically (the only deliberate key reuse, SK-01.3) | Same transaction ID twice, `Idempotent-Replayed: true`, exactly one transaction exists |
| 4 | Send `Concurrency` transfers at the same instant with distinct keys; their total exceeds A's balance | Each is 201, 422 or 503; no negative balance; final balances equal initial minus the successful transfers |
| 5 | `POST /api/v1/admin/reconciliations` | `PASS`, 0 discrepancies |
| 6 | Print the table of initial, final and expected balances and the reconciliation result (SK-01.4) | Overall PASS only if 1-5 passed |

Exit code 0 = PASS, 1 = FAIL, 2 = refused (unsafe target or missing credentials).

## Evaluation cases (-Evaluate, TQ-7)

E01 valid transfer, E02 insufficient balance, E03 fractional amount, E04 zero, E05 negative, E06 nonexistent source, E07 nonexistent destination, E08 same wallet, E09 duplicate request, E10 same key with a different payload, E11 boundary values (1 paisa and limit + 1), E12 missing key, E13 frozen destination, E14 balances and reconciliation after all cases. Database failure mid-transfer (atomicity) cannot be injected into a running server safely; it is covered by the automated `TransferRollbackTest` and reported as NOT RUN here. Results are recorded in `docs/testing/skill-evaluations.md` (Stage 3) and `docs/testing/stage-4-skill-evaluation.md` (Stage 4).

## Failure handling

| Symptom | Meaning / action |
|---|---|
| "Refusing to run against ..." | Only localhost is allowed. Start a local server. |
| "Admin login failed" | The env variables differ from the ones the server was started with. |
| Step 3 FAIL | Idempotency is broken - stop and open a defect issue. |
| Step 4 FAIL | Concurrency control is broken (negative or mismatched balance) - critical defect. |
| Step 5 FAIL | Reconciliation found discrepancies - inspect the report's discrepancy list. |
| Many 503 in step 4 | Write lock busy longer than 5 s; acceptable per SRS, but investigate if all fail. |

## Safety

- Never point it at a shared or real system: only `localhost` is accepted, all users are synthetic (`@simulate.test`), and money is simulated (admin test funding).
- Credentials come from environment variables, never from files or the command line history of a shared machine.
- Use a throw-away database file (`--ledger.db.path=target/simulate.db`); delete it afterwards.

## Grounding

The checks encode [[api-idempotency-and-retry-semantics]] (step 3), [[sqlite-write-lock-and-driver-behaviour]] (step 4), [[financial-reconciliation-and-auditability]] (step 5) and the testing approach in [[testing-concurrency-and-failure-injection]].
