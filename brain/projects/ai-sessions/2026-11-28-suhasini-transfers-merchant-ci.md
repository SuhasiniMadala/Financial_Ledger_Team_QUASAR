---
title: "AI session 2026-11-28: Stage 4 audit, transfer/merchant screens, CI and skill run (M3)"
type: ai-session
authors: [Suhasini M]
created: 2026-11-28
tags: [ai-session, stage-4]
requirements: [FR-TRF-01, FR-TRF-05, FR-PAY-01, FR-PAY-07, FR-LED-11, FR-LED-12, SK-01, UI-04, UI-05]
---

# AI session: Stage 4 audit, transfer and merchant screens, CI and skill run (Member 3 slice)

SDLC 6: significant AI sessions are recorded with goal, what worked and what failed.

## Context

Claude Code session on 2026-11-28 on Suhasini's laptop. It audited the repository against SRS v1.2 and the SDLC Stage 4 list, and after the team approved D1-D6 it enabled branch protection on `main`, generated the client for all three slices, and ran the backend tests, the client tests, the real-backend integration test and `simulate-transfer -Evaluate`. No secrets were pasted; the admin passwords used were throw-away local ones.

## Goal

Audit (`docs/review/stage-4-audit.md`); USER Transfer, Pay Merchant, History & Detail; MERCHANT Account, Payments, Statistics; two CI jobs for the client; the real-backend integration test; the Stage 4 simulate-transfer evaluation; the demo script.

## What worked

- A fresh `mvn -B verify` before planning (153 tests, BUILD SUCCESS) instead of trusting the Stage 3 report.
- The integration test drives the real backend through the client's own API modules and checks stored balances, ending with reconciliation PASS (AGENTS.md rule), so client/server contract drift is caught in CI.
- `simulate-transfer -Evaluate` run for real: steps 1-6 PASS, E01-E14 PASS, E15 NOT RUN by design; S1/S2 refusals exit 2 (`docs/testing/stage-4-skill-evaluation.md`).

## What failed or needed correction

- A `vite preview` process started through `npx` survived stopping its task and kept port 4173; found by checking the port and stopped by PID (same class of problem as DEF-005).
- The skill's instructions hard-coded `financial-ledger-0.3.0-SNAPSHOT.jar`, which would break when the release changes the version; replaced with a wildcard.
- Integration tests are skipped locally without admin credentials; in CI a missing credential now fails the job instead of silently skipping.
- The first CI run of PR #38 failed in `Frontend (lint, types, tests, build)`: the job used Node 20, but Vitest 5 and jsdom 31 need Node 22.22+/24.15+, so no test worker could start (my laptop has Node 24, so it passed locally). Fixed by moving CI to Node 24 and correcting `engines` and the docs (DEF-007).

## Reviewer notes (Suhasini) - fill in yourself, do not tick what you did not do

- [ ] I read every file in `frontend/src/features/user-payments/`, `frontend/src/features/merchant/`, the CI jobs and the integration test, and can explain them.
- [x] `cd frontend; npm ci; npm run lint; npm run typecheck; npm test`: all passed (111 tests in 12 files), and `npm run build` OK. Run on my laptop in the Claude Code session on 2026-11-28 on branch `stage4/member-3-transfer-review` (after G1 was merged), together with `mvn -B verify` (154 tests, 0 failures, BUILD SUCCESS) and `npm run test:integration` against a fresh local backend (6 passed; the 1 skipped test is the CI-only credentials check).
- [ ] I ran my demo scenarios (`docs/review/demos/member-3-transfers-payments.md`) including the stop-backend-and-Retry replay.
- [ ] Stage 3 note: ticked or answered the two open review lines in my 2026-11-27 AI-session note.
- [ ] Questions or changes I made after review: ...
