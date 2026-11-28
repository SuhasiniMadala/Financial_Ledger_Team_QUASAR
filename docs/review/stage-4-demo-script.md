# Stage 4 Review - Demonstration Script

Drafted by Suhasini M; each member owns and presents their own section. Target total: about 40 minutes. Rehearse the whole script at least once on the final `main` and record the rehearsal in each guide's **Actual** column.

Setup (before the reviewers arrive, 10 minutes): `docs/review/demo-environment.md` sections 2-4 on the presenting laptop: fresh demo database, backend in terminal 1, client in terminal 2, Obsidian open on `brain/`, GitHub open on the repository.

| # | Section | Presenter | Time | What happens | Guide / evidence |
|---|---|---|---|---|---|
| 1 | Introduction and architecture | Suhasini | 3 min | problem ("vanishing money"), modular monolith, single SQLite writer, ledger as source of truth, the three roles; show `docs/architecture/system-architecture.md` and SRS Figure B-2 | SRS 1.4, ADR-001..011 |
| 2 | Start-up check | Akshaya | 1 min | health URL shows UP; client login page | `demo-environment.md` |
| 3 | Authentication, wallets, admin wallet status | Akshaya | 5 min | scenarios 1-16 | `demos/member-1-auth-wallet.md` |
| 4 | Test funding and ledger entries | Geethika | 3 min | scenarios 1-6 | `demos/member-2-ledger.md` |
| 5 | Transfers, payments, idempotent retry, history, merchant views | Suhasini | 6 min | scenarios 1-14 (including the stop-backend-and-Retry replay) | `demos/member-3-transfers-payments.md` |
| 6 | Reversal, reconciliation, statistics | Geethika | 3 min | scenarios 7-12 | `demos/member-2-ledger.md` |
| 7 | simulate-transfer end-to-end with -Evaluate | Suhasini | 4 min | live run, explain steps 3-5 | `demos/member-3-transfers-payments.md`, `docs/testing/stage-4-skill-evaluation.md` |
| 8 | local-check live | Akshaya | 4 min | run the script, show the 5-step report and that the server stopped | `local-check-demonstration.md` |
| 9 | brain-query live | Geethika | 4 min | ask the fixed question, open the cited notes | `brain-query-demonstration.md` |
| 10 | Traceability walkthrough | Geethika (Akshaya shows the brain note she wrote, Suhasini the concurrency test she wrote) | 4 min | brain note -> SRS -> issue -> ADR -> code -> test -> PR | `traceability-walkthrough.md` |
| 11 | CI and release evidence | Suhasini | 3 min | green CI run on final `main` (all jobs incl. frontend), the `v1.0.0-stage4` tag and GitHub Release, CHANGELOG, Stage 4 review | `stage-4-review.md`, CHANGELOG |
| 12 | Questions | all | rest | each member answers about their own code | - |

## Recovery during the live demo

| Problem | Action | Who |
|---|---|---|
| Backend crashed or was stopped | restart terminal 1 with the same command (data is kept in the demo file); in the client press Retry on any pending money form | whoever is presenting |
| Client error page or blank screen | refresh the browser; if still broken, continue the same steps in Swagger UI (http://localhost:8080/swagger-ui.html) and say so | presenter |
| Demo data in a wrong state | reset (`demo-environment.md` section 6) and replay Member 1 scenarios 1-3, 8, 10 and Member 2 scenarios 1-2 (about 3 minutes) | Akshaya + Geethika |
| local-check slow (Maven download) | continue with section 9 and come back; show the CI run of the same script as backup evidence | Akshaya |
| simulate-transfer FAIL | do not hide it: read which step failed, show the report, record it, open a defect issue after the review | Suhasini |
| No network (GitHub unavailable) | show local `git log --oneline --graph`, the PR numbers in `traceability-walkthrough.md`, and saved CI screenshots | Suhasini |

## Rules for the day

- Only simulated data and synthetic `@example.test` people. No real passwords on screen; type the admin password with `Read-Host`.
- Report what actually happens. A failed step is recorded as FAIL, never re-labelled.
- Every member explains their own code and can describe the whole system at a high level (SDLC 6).
