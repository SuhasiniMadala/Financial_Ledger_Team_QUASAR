# Skill Evaluations (TQ-7)

Run before the Stage 3 submission on 2026-11-27, Windows 11, Windows PowerShell 5.1, Java 21. Reports were produced by the skills themselves. Re-run these before every stage submission and replace the results.

## local-check (SK-02) - owner Akshaya A

| ID | Scenario | Expected | Actual | Result |
|---|---|---|---|---|
| L0 | First run (before fixes) | All 5 steps PASS, server stopped | Steps ran, then the script crashed writing the report (DEF-004) and the server kept running (DEF-005) | FAIL -> defects logged and fixed |
| L1 | Normal run after fixes | Steps 1-5 PASS; port free and temp folder deleted afterwards | 1 PASS (build), 2 PASS (151 tests, 0 failures), 3 PASS (started in about 6 s), 4 PASS (`{"status":"UP","database":"UP"}`), 5 PASS; port 18080 free; 0 temp folders left | **PASS** |
| L2 | Port 18080 already occupied | Step 3 FAIL with a clear message; steps 4-5 NOT RUN; nothing left running | Step 3 FAIL "Port 18080 is already in use"; 4-5 NOT RUN; exit code 1; 0 leftover processes | **PASS** |
| L3 | Clean Linux machine (CI job `local-check`, pwsh 7) | All PASS | Pending - runs when the first Stage 3 PR is pushed | NOT RUN |

Command: `powershell -ExecutionPolicy Bypass -File brain/skills/local-check/scripts/local-check.ps1 -ReportPath target/skill-runs/local-check-report.md`

## simulate-transfer (SK-01) - owner Suhasini M

Run against a throw-away local server on port 18081 with a temporary database and a random session-only admin password. Amount 10000 paise, concurrency 10.

### Main workflow (SRS 6.3.1)

| Step | Result | Evidence |
|---|---|---|
| 1. Create wallets A and B, fund A | PASS | both ACTIVE; funding 100000 -> 201 |
| 2. Initial balances | PASS | A = 100000, B = 0 |
| 3. Transfer, then identical resend | PASS | same transaction ID, `Idempotent-Replayed: true`, 1 transaction on A |
| 4. 10 simultaneous transfers totalling 100000 > balance 90000 | PASS | 201 = 9, 422 = 1, 503 = 0; A = 0 (expected 0), B = 100000 (expected 100000) |
| 5. Reconciliation | PASS | PASS, 0 discrepancies |
| 6. Summary | PASS | overall PASS |

### Evaluation cases

| ID | Scenario | Input | Expected | Actual | Result |
|---|---|---|---|---|---|
| E01 | Valid transfer | 10000 paise E->F | 201; E=40000, F=10000 | 201; E=40000, F=10000 | PASS |
| E02 | Insufficient balance | 999999 paise | 422 INSUFFICIENT_FUNDS, FAILED txn, balance unchanged | 422, txn id returned, E=40000 | PASS |
| E03 | Fractional amount | 100.5 | 400 VALIDATION_ERROR | 400 VALIDATION_ERROR | PASS |
| E04 | Zero amount | 0 | 400 | 400 | PASS |
| E05 | Negative amount | -500 | 400 | 400 | PASS |
| E06 | Nonexistent source | W-0000000000 | 404 NOT_FOUND | 404 NOT_FOUND | PASS |
| E07 | Nonexistent destination | W-0000000000 | 422 INVALID_DESTINATION | 422 INVALID_DESTINATION | PASS |
| E08 | Same wallet (BR-06) | E->E | 422 SAME_WALLET | 422 SAME_WALLET | PASS |
| E09 | Duplicate request | same key twice | same ID, replayed, charged once | same=True, replayed=True, E=39900 | PASS |
| E10 | Same key, different payload | reuse key with 200 | 409 IDEMPOTENCY_KEY_REUSED | 409 | PASS |
| E11 | Boundaries | 1 paisa; limit + 1 | 201; 400 | 201; 400 | PASS |
| E12 | Missing key | no header | 400 IDEMPOTENCY_KEY_REQUIRED | 400 | PASS |
| E13 | Frozen destination (BR-07) | freeze F, transfer | 422 WALLET_NOT_ACTIVE | 422 | PASS |
| E14 | Consistency after all cases | balances + reconciliation | E=39899, PASS | E=39899, PASS | PASS |
| E15 | Database failure mid-transfer | - | rollback, no partial effect | cannot be injected into a running server safely; covered by `TransferRollbackTest` (PASS) | NOT RUN in skill |

### Safety and robustness evaluations

| ID | Scenario | Expected | Actual | Result |
|---|---|---|---|---|
| S1 | Base URL `https://example.com` | refuse, exit 2 | "REFUSED: will not run against 'example.com'", exit 2 | PASS (after DEF-006 fix; first run exited 1) |
| S2 | Admin credentials not set | refuse, exit 2 | "REFUSED: set LEDGER_ADMIN_EMAIL and LEDGER_ADMIN_PASSWORD", exit 2 | PASS |
| S3 | Report shows evaluation inputs | Input column filled | first run empty (DEF-006); after fix filled | PASS after fix |

## Brain skills - owner Geethika M

| ID | Skill | Scenario | Expected | Actual | Result |
|---|---|---|---|---|---|
| B1 | brain-refine | Validate the Stage 2 vault | Validator reports real problems | 24 notes without frontmatter, 18 broken links, skills without citations, `simulate-transfer` missing | PASS (problems detected) |
| B2 | brain-refine | Refine then validate | `BrainVaultTest` 4/4 and `SkillDefinitionsTest` 3/3 pass | 4/4 and 3/3 pass | PASS |
| B3 | brain-ingest | New notes follow the schema | 9 new Stage 3 notes accepted by the validator | accepted | PASS |
| B4 | brain-query | "Why must the idempotency lookup happen after BEGIN IMMEDIATE?" | Answer cites notes, requirement IDs, ADR, code and test | Answer recorded in `brain/skills/brain-query/SKILL.md` with 3 note citations, FR-TRF-07/FR-CON-02/FR-CON-08, ADR-005/009, code and test | PASS |

Note on B4: brain-query is an instruction skill executed by an AI agent or a person, so its evaluation is a recorded example run, not an automated test.
