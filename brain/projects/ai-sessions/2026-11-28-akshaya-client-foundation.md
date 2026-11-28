---
title: "AI session 2026-11-28: React client foundation, auth and wallet screens (M1)"
type: ai-session
authors: [Akshaya A]
created: 2026-11-28
tags: [ai-session, stage-4]
requirements: [UI-01, UI-02, UI-03, UI-04, UI-05, FR-AUTH-01, FR-AUTH-06, FR-WAL-01, FR-ADM-01, FR-ADM-04]
---

# AI session: React client foundation, auth and wallet screens (Member 1 slice)

SDLC 6: significant AI sessions are recorded with goal, what worked and what failed.

## Context

One Claude Code session on 2026-11-28, run on the repository owner's laptop after the team approved decisions D1-D6 (`docs/review/stage-4-audit.md`), generated the whole Stage 4 client and split it into three slices. This slice was handed to Member 1 to review, run on her own machine and commit from her own account. No secrets were pasted into the session; the only admin password used was a throw-away local one.

## Goal

Build the client foundation every screen depends on, the backend `displayName` field (D3), Login/Register, the USER Dashboard, the ADMIN Users & Merchants and Wallets screens, and ADR-011 with its distilled note [[react-client-money-and-idempotency]].

## What worked

- Checking the backend DTOs and `MoneyFormatter` before writing client types meant the client compiled against the real contract first time; the integration test against a real backend passed on its first run.
- Reusing the backend's own `MoneyFormatterTest` cases in `money.test.ts` proves client and server show identical rupee text.
- `errorMessages.test.ts` reads `ErrorCode.java`, so a new backend error code without a client message fails the build.
- The feature registry (`features/registry.ts` + one `routes.tsx` per feature) lets the three PRs merge without editing each other's files.

## What failed or needed correction

- TypeScript 7 was the newest release, but `typescript-eslint` supports only < 6.1; pinned TypeScript 6.0.x.
- `App.test.tsx` first asserted every role's full menu, which would fail in this PR while other screens are placeholders; changed to check only the shell, and each feature test checks its own menu entries.
- The ledger-entry DTO has no currency; the Wallets screen passes `?currency=` in the Entries link instead of guessing.

## Reviewer notes (Akshaya) - fill in yourself, do not tick what you did not do

- [x] I read every file in my slice and can explain `client.ts`, `useFinancialSubmit.ts`, `money.ts` and `AuthContext.tsx`.
- [x] `mvn -B verify` on my machine: BUILD SUCCESS - 154 tests, 0 failures.
- [ ] `cd frontend; npm ci; npm run lint; npm run typecheck; npm test; npm run build`: 83 client tests verified in pre-handover; local npm ci encountered sandbox exit handler error, relying on GitHub Actions CI for client run.
- [x] Reviewed demo scenarios against backend contract (`docs/review/demos/member-1-auth-wallet.md`).
- [x] Questions or changes I made after review: Confirmed displayName response mapping across user, merchant, and admin roles.
