---
title: "Project: Stage 4 - React client and final review"
type: project
authors: [Suhasini M]
created: 2026-11-27
tags: [stage-4]
requirements: [UI-01, UI-02, UI-03, UI-04, UI-05, SK-01, SK-02]
---

# Project: Stage 4 - React client and final review

Active project note (PARA "projects"). Audit, decisions and work split: `docs/review/stage-4-audit.md`. All Stage 4 review evidence lives in `docs/review/`.

## Deliverables (SDLC 7, Stage 4 - Review)

- Live demonstration of the FRs in the React client (SRS 3.1, UI-01..UI-05) - `frontend/`
- `simulate-transfer` end-to-end, `local-check` live, `brain-query` live
- One requirement walked from brain note to ADR, code and test: FR-CON-08 / FR-TRF-05 via [[api-idempotency-and-retry-semantics]] -> ADR-005 -> `IdempotencyService.execute` -> `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` -> client `useFinancialSubmit` (UI-04). Written up in `docs/review/traceability-walkthrough.md`.
- Final release tag `v1.0.0-stage4` (decision D4), only after every check passes on `main`

## Work split (decision D6)

| Member | Area | PRs |
|---|---|---|
| Akshaya A (M1) | backend `displayName`, client foundation, Login/Register, USER Dashboard, ADMIN Users & Merchants and Wallets, ADR-011, local-check demo | A1 |
| Geethika M (M2) | ADMIN test funding, Transactions & Ledger, reversal, Reconciliation, Statistics; demo environment, brain-query demo, traceability walkthrough | G1 |
| Suhasini M (M3) | audit; USER Transfer, Pay Merchant, History; MERCHANT screens; frontend CI; simulate-transfer evaluation; demo script; review and release | P0, S1, R1 |

## Decisions (2026-11-28)

- D1 build the React client; D2 the old `docs/stage-4/` plan stays deleted; D3 `displayName` in the login response; D4 tag `v1.0.0-stage4`; D5 branch protection on `main` (enabled); D6 the split above.
- ADR-011 (Akshaya): React + Vite, `/api` proxy instead of CORS, `sessionStorage` session, money as integer paise in the client (distilled note `brain/resources/react-client-money-and-idempotency.md`, added with ADR-011).
- Money stays integer paise end to end ([[monetary-representation-and-integer-units]]); the client never computes balances.

Previous stage: [[stage-3-testing-and-ci]], [[stage-3-review]].
