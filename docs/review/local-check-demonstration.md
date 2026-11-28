# local-check - Live Demonstration (SK-02, SDLC 3)

Owner: Akshaya A (author of the skill in Stage 3, issue #6). Presented live in the review; time about 3 minutes (the build and tests take most of it).

## What it proves

On the presenter's own machine, from a clean checkout of `main`: (1) the project builds, (2) all automated tests pass, (3) the server starts on a temporary database within 60 seconds, (4) the read-only `GET /api/v1/health` answers UP, (5) a PASS/FAIL report is printed, and the server it started is stopped (SK-02.1, SK-02.2). No credentials are needed and no persistent data is touched.

## Command (repository root, PowerShell)

```powershell
git switch main
git pull origin main
powershell -ExecutionPolicy Bypass -File brain/skills/local-check/scripts/local-check.ps1 -ReportPath target/local-check-report.md
```

Expected end: a table with steps 1-5 all `PASS` and `Overall: PASS`. If a step fails, later steps show `NOT RUN` with the error (SK-02.1). Afterwards `Get-NetTCPConnection -LocalPort 18080 -State Listen` returns nothing (the server was stopped, DEF-005).

## Recorded runs

Paste the report table from `target/local-check-report.md` after each run. Only record runs you actually did.

| Date | Who | Commit (`git rev-parse --short HEAD`) | Step 1 Build | Step 2 Tests | Step 3 Start | Step 4 Health | Overall | Server stopped |
|---|---|---|---|---|---|---|---|---|
| | Akshaya (rehearsal) | | | | | | NOT YET RUN | |
| | Akshaya (live review) | | | | | | NOT YET RUN | |

CI also runs the same script on a clean Linux machine on every PR (job "local-check skill on a clean machine"); link the run of the final `main` here: (fill in).

## If it fails live

1. Read the step that failed and its error lines in the report.
2. Build or test failure: run `mvn -B verify` to see the failing test; do not skip or weaken it (AGENTS.md).
3. Start failure: port 18080 busy -> rerun with `-Port 18090`.
4. Record the failure honestly in the table above, fix it in a PR with a regression test if it is a defect (TQ-8), then rerun.
