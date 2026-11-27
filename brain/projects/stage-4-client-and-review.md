---
title: "Project: Stage 4 - React client and final review"
type: project
authors: [Suhasini M]
created: 2026-11-27
tags: [stage-4]
requirements: [UI-01, UI-02, UI-03, UI-04, UI-05, SK-01, SK-02]
---

# Project: Stage 4 - React client and final review

Active project note (PARA "projects"). Plan, owners and acceptance criteria: `docs/stage-4/MASTER_PLAN.md` (PR from `stage4/member-3-plan`).

## Deliverables (SDLC 7, Stage 4 - Review)

- Live demonstration of the FRs in a React client (SRS 3.1, UI-01..UI-05)
- `simulate-transfer` end-to-end, `local-check` live, `brain-query` live
- One requirement walked from brain note to ADR, code and test: FR-CON-08 via [[api-idempotency-and-retry-semantics]] -> ADR-005 -> `IdempotencyService` -> `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` -> `useFinancialSubmit` (UI-04)
- Final release tag (proposed `v1.0.0-stage4`, needs team approval)

## Work split

| Member | Area | PRs |
|---|---|---|
| Akshaya A (M1) | client foundation, login/register, USER dashboard, transfer, pay merchant | A1, A2 |
| Geethika M (M2) | ADMIN console: directory, wallets, funding, transactions & ledger, reversal, reconciliation, statistics | G1, G2 |
| Suhasini M (M3) | plan, frontend CI + integration harness, USER history, MERCHANT screens, docs and release | P0, S1, S2, S3 |

## Key decisions to record

- ADR-011 (Akshaya): React + Vite, `/api` proxy instead of CORS, `sessionStorage` session, money as integer paise in the client.
- Money stays integer paise end to end ([[monetary-representation-and-integer-units]]); the client never computes balances.
- Every financial form reuses one idempotency hook so a retry after a timeout or 503 resends the same key ([[api-idempotency-and-retry-semantics]]).

## Open items found in the audit (2026-11-27)

- SRS v1.4 and the Project Statement were not available; the repository has SRS v1.2.
- The login response has no display name (gap G-1).
- `main` has no branch protection.

Previous stage: [[stage-3-testing-and-ci]], [[stage-3-review]].
