---
title: "Standing rules: testing strategy and quality gates"
type: area
authors: [Geethika M]
created: 2026-11-27
tags: [testing, quality, tq]
requirements: [TQ-1, TQ-2, TQ-3, TQ-4, TQ-5, TQ-6, TQ-7, TQ-8, NFR-TEST-01, NFR-TEST-07]
---

# Standing rules: testing strategy and quality gates

The full plan is `docs/testing/test-plan.md`; this note is the short version every member keeps applying.

## Gates a change must pass before merge

1. `mvn -B verify` is green: Checkstyle lint, all tests, and the JaCoCo rule (>= 70% line coverage of the ledger, transfer, idempotency and reconciliation packages, NFR-TEST-07).
2. CI on the pull request is green (build, lint, tests, secret scan, skill and brain validation).
3. A teammate reviewed the diff and can explain it.

## How we test money

- Never trust only the status code: check `balance_minor`, the ledger entries and, at the end, reconciliation PASS (NFR-SAFE-02). Patterns are described in docs/testing/test-plan.md.
- Integer minor units only. `ArchitectureRulesTest` fails the build if `float`/`double` appears in production code (TQ-6, [[monetary-representation-and-integer-units]]).
- Boundaries for every amount field: 0, 1, -1, the limit, the limit + 1, a fraction, a string.

## When a defect is found (TQ-8)

1. Write a failing test that reproduces it.
2. Fix the cause, not the test.
3. Keep the test as the regression test and name the defect ID in its `@DisplayName` or comment.
4. Add a row to `docs/testing/defect-log.md` and open/close the GitHub issue.

## Before each stage submission (TQ-7)

Run the skill evaluations (`local-check`, `simulate-transfer -Evaluate`, brain skills) and record the real results in `docs/testing/skill-evaluations.md`.
