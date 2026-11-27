---
name: local-check
description: Verify that the Financial Ledger builds, passes its automated tests, starts on a temporary database and answers the read-only GET /api/v1/health on this machine, reporting PASS or FAIL for each of the five steps. Use after cloning, after pulling changes, before opening a pull request, and live in the Stage 4 review.
compatibility: Requires Java 17+, Maven 3.8+, Git, and Windows PowerShell 5.1 or PowerShell 7 (pwsh). Network access to Maven Central for the first build.
---

# local-check (SRS SK-02)

## Purpose

Prove on a team member's own machine that the project works end to end, exactly as defined in SRS 6.3.2 and SDLC section 3. It never modifies persistent data: the application runs against a temporary SQLite file that is deleted afterwards (SK-02.2).

## Inputs

| Parameter | Default | Meaning |
|---|---|---|
| `-Port` | 18080 | Port for the temporary server (avoids clashing with a normal run on 8080) |
| `-StartupTimeoutSeconds` | 60 | SK-02 step 3 pass condition: ready within 60 seconds |
| `-ReportPath` | none | Optional file to save the Markdown report |

No credentials are needed. The server uses a random per-process JWT key (`--ledger.jwt.allow-ephemeral-secret=true`), so no secret is stored or typed.

## Steps (run from the repository root)

Windows (VS Code terminal, PowerShell):

```powershell
powershell -ExecutionPolicy Bypass -File brain/skills/local-check/scripts/local-check.ps1
```

Linux / macOS / CI:

```bash
pwsh brain/skills/local-check/scripts/local-check.ps1
```

The script performs, in order:

1. **Build** - `mvn -B -DskipTests package`. PASS when the exit code is 0.
2. **Automated tests** - `mvn -B test` (lint, unit, integration, API, concurrency and durability tests). PASS when all tests pass; the report shows the `Tests run` summary.
3. **Start the application** - `java -jar target/financial-ledger-*.jar` with `--ledger.db.path=<temp folder>` on `-Port`. PASS when `/api/v1/health` answers 200 within the timeout. FAIL immediately if the port is already in use.
4. **Read-only fetch** - `GET http://localhost:<port>/api/v1/health`. PASS on HTTP 200 with `"status":"UP"` (the endpoint reads the database, NFR-REL-02). No financial endpoint is ever called.
5. **Stop and clean up** - stop the process and delete the temporary folder.

The script stops at the first failure and marks later steps `NOT RUN` with the error output (SK-02.1). Cleanup (stopping the application) always happens, even after a failure (SK-02.2).

## Expected output

```
| Step | Result | Details |
| 1. Build the project | PASS | mvn -B -DskipTests package: exit code 0 |
| 2. Run the automated test suite | PASS | mvn -B test: Tests run: ..., Failures: 0, Errors: 0, Skipped: 0 |
| 3. Start the application (temporary database) | PASS | Started financial-ledger-0.3.0-SNAPSHOT.jar on port 18080 in about 6 s |
| 4. GET /api/v1/health (read-only) | PASS | HTTP 200 {"status":"UP","database":"UP"} |
| 5. Stop the application and clean up | PASS | Application stopped; temporary database deleted |
**Overall: PASS**
```

Exit code 0 means overall PASS; 1 means FAIL.

## Failure handling

| Symptom | Likely cause | What to do |
|---|---|---|
| Step 1 FAIL, "mvn is not recognized" | Maven not installed or not on PATH | Install Maven, reopen the terminal, run `mvn -v` |
| Step 1 FAIL with Checkstyle violations | lint rule broken | Fix the listed file and line |
| Step 2 FAIL | a test failed | Read the test name in the output; run `mvn test -Dtest=ThatTest` |
| Step 3 FAIL "Port ... in use" | another server is running | Stop it, or pass `-Port 18090` |
| Step 3 FAIL "Not ready" | start-up error | Read the log lines printed in the report |
| "running scripts is disabled" | PowerShell execution policy | Use the `-ExecutionPolicy Bypass` command above |

## Safety

Read-only endpoint only; temporary database only; no secrets; always stops the server it started.

## Evaluations (TQ-7)

Recorded in `docs/testing/skill-evaluations.md`: normal run (expected all PASS) and an occupied port (expected step 3 FAIL, steps 4-5 NOT RUN, server not started).

## Grounding

The health check must read the database because SQLite only reports a problem when it is actually used ([[sqlite-concurrency-and-acid-transactions]]). Requirement IDs: SK-02.1, SK-02.2, NFR-REL-02.
