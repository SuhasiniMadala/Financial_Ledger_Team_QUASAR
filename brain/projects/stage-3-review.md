---
title: "Stage 3 review (Testing and CI)"
type: review
authors: [Suhasini M]
created: 2026-11-27
tags: [stage-3, review, sb-2]
---

# Stage 3 review (Testing and CI)

SB-2 review note: what worked, what did not, what to change. Evidence: `docs/testing/test-execution-report.md`.

## What worked

- Restarting from the end of Stage 2 instead of patching the unreviewed `stage3-handoff` code: the audit had shown that its passing tests missed a double-charge race, so we rebuilt with tests written against the SRS scenarios.
- Tests that check stored balances and end with reconciliation PASS, plus real concurrency, failure injection, a busy-lock test and a kill-and-restart durability test ([[testing-concurrency-and-failure-injection]]).
- Checking the driver before trusting the design (ADR-009, [[sqlite-write-lock-and-driver-behaviour]]).
- Validators that really fail: on their first run they reported 24 notes without metadata, 18 broken links and skills without citations.

## What did not work

- Three defects in our own test and documentation setup (DEF-001 to DEF-003) - found by tests, but they cost time.
- The whole implementation came out of one AI session on one laptop; each member must still review and be able to explain their slice before committing it (SDLC 6). The AI-session notes have a checklist for that.
- Stage 1 and 2 review notes were written late, at the start of Stage 3.

## What to change for Stage 4

- Open small pull requests early and review each other's within a day.
- Build the minimal React client (UI-01..UI-05) on top of the documented API.
- Keep the traceability matrix updated in the same PR as each change.
