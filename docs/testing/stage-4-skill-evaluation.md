# Stage 4 Skill Evaluation - simulate-transfer (SK-01, TQ-7)

Owner: Suhasini M. Stage 3 results stay in `skill-evaluations.md`; this file records the Stage 4 runs. Each run is pasted from the script's own report (`-ReportPath`), not retyped.

## Run 1 - pre-integration (2026-11-28 00:37 IST)

| Item | Value |
|---|---|
| Code | `stage4/member-3-plan` @ `46abea0` plus the uncommitted Stage 4 working tree (backend change: `displayName` in the login response only; transfer, ledger and idempotency code identical to `main` @ `b1be847`) |
| Server | `java -jar target/financial-ledger-0.3.0-SNAPSHOT.jar --server.port=18086` on a throw-away SQLite file, random per-process JWT key, synthetic local admin |
| Command | `powershell -ExecutionPolicy Bypass -File brain/skills/simulate-transfer/scripts/simulate-transfer.ps1 -BaseUrl http://localhost:18086 -Evaluate -ReportPath target/simulate-transfer-report.md` |
| Exit code | 0 |
| Run by | Suhasini M's machine, with Claude Code executing the command (output below is unedited) |

| Step | Result | Details |
|---|---|---|
| 1. Create wallets A and B, fund A | PASS | A=W-1CAPPFRDRX B=W-N8ERTHQ637 ACTIVE; test funding 100000 -> HTTP 201 |
| 2. Record initial balances | PASS | A=100000 B=0 |
| 3. Transfer, then resend identically | PASS | first 201 T-CP0GSD3FJSRY; resend 201 replayed=True; transactions on A: 1 |
| 4. 10 simultaneous transfers (total 100000 > balance 90000) | PASS | 201=9 422=1 503=0 other=0; A=0 (expected 0) B=100000 (expected 100000) |
| 5. Reconciliation | PASS | HTTP 200 result=PASS discrepancies=0 |
| 6. Summary | PASS | Overall PASS only if steps 1-5 passed |

| Wallet | Initial | After step 3 | Final | Expected final |
|---|---|---|---|---|
| A W-1CAPPFRDRX | 100000 | 90000 | 0 | 0 |
| B W-N8ERTHQ637 | 0 | 10000 | 100000 | 100000 |

### Evaluation cases (TQ-7)

| ID | Scenario | Input | Expected | Actual | Result |
|---|---|---|---|---|---|
| E01 | Valid transfer | 10000 paise E->F | 201; E=40000, F=10000 | 201; E=40000, F=10000 | PASS |
| E02 | Insufficient balance | 999999 paise | 422 INSUFFICIENT_FUNDS, FAILED txn id, balance unchanged | 422 INSUFFICIENT_FUNDS txn=T-ZB1KY2MGTX68; E=40000 | PASS |
| E03 | Invalid amount (fraction) | amountMinor 100.5 | 400 VALIDATION_ERROR | 400 VALIDATION_ERROR | PASS |
| E04 | Zero amount | amountMinor 0 | 400 VALIDATION_ERROR | 400 VALIDATION_ERROR | PASS |
| E05 | Negative amount | amountMinor -500 | 400 VALIDATION_ERROR | 400 VALIDATION_ERROR | PASS |
| E06 | Nonexistent source account | source W-0000000000 | 404 NOT_FOUND, nothing recorded | 404 NOT_FOUND | PASS |
| E07 | Nonexistent destination account | destination W-0000000000 | 422 INVALID_DESTINATION | 422 INVALID_DESTINATION | PASS |
| E08 | Same-account transfer (BR-06) | E->E | 422 SAME_WALLET | 422 SAME_WALLET | PASS |
| E09 | Duplicate request (same key, same body) | same key twice | same transactionId, Idempotent-Replayed, charged once | 201/201 same=True replayed=True E=39900 | PASS |
| E10 | Same key, different payload | reuse E09 key with 200 | 409 IDEMPOTENCY_KEY_REUSED | 409 IDEMPOTENCY_KEY_REUSED | PASS |
| E11 | Boundary values | 1 paisa; limit+1 | 201; 400 | 201; 400 | PASS |
| E12 | Missing Idempotency-Key | no header | 400 IDEMPOTENCY_KEY_REQUIRED | 400 IDEMPOTENCY_KEY_REQUIRED | PASS |
| E13 | Destination wallet FROZEN (BR-07) | freeze F, transfer | 422 WALLET_NOT_ACTIVE | 422 WALLET_NOT_ACTIVE | PASS |
| E14 | Balances consistent after all cases | final balances + reconciliation | E=39899, reconciliation PASS | E=39899, PASS | PASS |
| E15 | Database failure mid-transfer (atomicity) | - | covered by automated test TransferRollbackTest | not injectable against a running server | NOT RUN |

**Overall: PASS** (E15 NOT RUN by design; atomicity is verified by `TransferRollbackTest` and `RollbackFailureInjectionTest` in `mvn -B verify`).

### Safety cases

| ID | Input | Expected | Actual | Result |
|---|---|---|---|---|
| S1 | `-BaseUrl https://example.com` | refuse, exit 2 | "REFUSED: will not run against 'example.com'. This skill only runs against a local test instance.", exit 2 | PASS |
| S2 | admin variables not set | refuse, exit 2 | "REFUSED: set LEDGER_ADMIN_EMAIL and LEDGER_ADMIN_PASSWORD (the admin the local server was started with).", exit 2 | PASS |

## Run 2 - final `main` before the release tag

Required before `v1.0.0-stage4` (TQ-7: skill evaluations run before each stage submission). Paste the report of the run on the final merged `main` here.

| Item | Value |
|---|---|
| Commit | (fill in) |
| Exit code | (fill in) |
| Overall | NOT YET RUN |

## Run 3 - live in the review

| Item | Value |
|---|---|
| Date / presenter | (fill in) |
| Overall | NOT YET RUN |
