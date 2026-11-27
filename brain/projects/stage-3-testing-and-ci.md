---
title: "Project: Stage 3 - Testing and CI"
type: project
authors: [Suhasini M]
created: 2026-11-27
tags: [stage-3]
requirements: [TQ-7, TQ-8, SK-01, SK-02]
---

# Project: Stage 3 - Testing and CI

Active project note (PARA "projects"). Deadline: Stage 3 submission. Plan details: `docs/architecture/development-plan.md`.

## Deliverables (SDLC 7, Stage 3)

- Continuous testing and CI plan/results -> `docs/testing/test-plan.md`, `docs/testing/test-execution-report.md`, `docs/ci/ci-pipeline.md`
- Product skill with test results -> `brain/skills/simulate-transfer/`, `docs/testing/skill-evaluations.md`
- Defect log with regression tests -> `docs/testing/defect-log.md`
- Review note for each stage -> [[stage-1-review]], [[stage-2-review]], [[stage-3-review]]

## Work split (one functional area each)

| Member | Area | Branch |
|---|---|---|
| Akshaya A (M1) | Accounts, wallets, admin directory, health, security, local-check, lint | `stage3/member-1-account` |
| Geethika M (M2) | Ledger engine, idempotency, funding, reversals, reconciliation, brain refine | `stage3/member-2-payments-ledger` |
| Suhasini M (M3) | Transfers, payments, merchant views, history, concurrency/durability/perf, simulate-transfer, CI | `stage3/member-3-transfers-history` |

Merge order is M1 -> M2 -> M3 because each module builds on the previous one.

## Decisions taken this stage

ADR-008 (currencies and limit), ADR-009 (write lock), ADR-010 (test databases). Knowledge: [[sqlite-write-lock-and-driver-behaviour]], [[testing-concurrency-and-failure-injection]].
