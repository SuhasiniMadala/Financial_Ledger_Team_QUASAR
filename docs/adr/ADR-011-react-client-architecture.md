# ADR-011: Minimal React Client - Vite Proxy, Integer Money and One Idempotent Submit Hook

## Status
Accepted

## Date
2026-11-28

## Context
SRS 3.1 and UI-01..UI-05 require a minimal React single-page app with a top bar (product name, account name, Logout), role-specific navigation and 12 role screens. SDLC 7 requires the Stage 4 demonstration of the FRs "in the client". The backend (Stage 3) is complete and must not be weakened: it has no CORS configuration, its login response had no account name, and its money rules (integer paise, Idempotency-Key, 503 SERVICE_BUSY with Retry-After) must hold in the browser too.

Options considered:
1. **Vite + React + TypeScript, same-origin proxy (chosen).** Small, standard tooling; the proxy avoids any backend CORS change.
2. Serve the built client from Spring Boot (`src/main/resources/static`). One process, but couples the Maven build to Node, and a client change would require a backend rebuild.
3. Add CORS to the backend and serve the client separately. Adds a security surface (allowed origins, credentials) that the SRS does not need.

## Decision
1. **Stack:** React + TypeScript built with Vite in `frontend/`; `react-router-dom`; Vitest + React Testing Library; ESLint. No UI kit, no HTTP or money library.
2. **Transport:** the client calls relative `/api/v1/...` URLs; the Vite dev and preview servers proxy `/api` to the backend (`LEDGER_API_URL`, default `http://localhost:8080`). No backend CORS.
3. **Account name:** the login response gains one field, `displayName` (decision D3). No new endpoint; the JWT is unchanged.
4. **Money:** amounts are integer paise end to end. Typed rupees are parsed by string manipulation; display uses the same algorithm as the backend `MoneyFormatter` and is tested against its cases. `parseFloat`, `toFixed` and division by 100 are forbidden by lint and by a test (TQ-6).
5. **Idempotency:** one hook, `useFinancialSubmit`, is used by every money form (transfer, payment, test funding, reversal). New submission = new UUID key; retry after a network error, timeout or 503 = same key and body; the button is disabled and re-entry ignored while in flight (UI-04, UI-05).
6. **Errors:** every code of SRS Table 3-2 maps to a sentence (UI-03); a test reads the backend's `ErrorCode.java` so a new backend code without a client message fails the build.
7. **Feature ownership:** each feature folder exports its routes and navigation (`features/<feature>/routes.tsx`), collected in `features/registry.ts`, so members never edit each other's files.

Rules and reasoning: [[react-client-money-and-idempotency]], [[api-idempotency-and-retry-semantics]], [[monetary-representation-and-integer-units]].

## Consequences
- Positive: no backend change except one additive response field; all money rules are enforced twice (client pre-check, backend authority).
- Positive: the client's API modules are plain functions, so the same code runs in component tests (fake client) and in integration tests against the real backend.
- Negative: the client needs Node 24.15+ (or 22.22+, required by Vitest 5 and jsdom) and a second process (`npm run dev` or `npm run preview`) next to the backend.
- Negative: the session token lives in `sessionStorage` and is readable by script in the page; accepted for this academic, single-origin client with no third-party scripts and 60-minute tokens.
