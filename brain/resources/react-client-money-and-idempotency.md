---
title: "React Client: Money, Idempotent Submission and Sessions"
type: resource
authors: [Akshaya A]
created: 2026-11-28
tags: [stage-4, frontend, idempotency, money]
requirements: [UI-01, UI-02, UI-03, UI-04, UI-05, TQ-6, FR-TRF-05, NFR-SEC-02]
---

# React Client: Money, Idempotent Submission and Sessions

Distilled rules for the Stage 4 React client (`frontend/`), cited by ADR-011. They apply the backend rules of [[api-idempotency-and-retry-semantics]] and [[monetary-representation-and-integer-units]] to a browser client.

## 1. Money stays integer paise in the browser (UI-01, UI-02, TQ-6)

- A typed amount like `250.50` is converted by **string parsing**: split on the point, pad the paise to two digits and join the digit strings (`"250" + "50"` -> `25050`). `parseFloat("0.29") * 100` gives `28.999999999999996`, which is exactly the error the backend avoids.
- More than two decimals, signs, exponents (`1e3`), zero and amounts above the per-transaction limit (BR-02) are rejected before anything is sent. The backend validates again.
- Display uses the same algorithm as the backend `MoneyFormatter` (rupee sign, Indian grouping `1,00,000.00`, two decimals). The client test suite runs the backend's own cases, so both always show the same text.
- Guards: an ESLint rule forbids `parseFloat`; a test fails if any source file uses `toFixed(` or divides by 100.
- The client **never computes a balance**. After every money action it shows the backend's 201 receipt and re-reads balances.

## 2. One idempotent submission hook for every money form (UI-04, UI-05)

| Situation | Key sent |
|---|---|
| New submission | new `crypto.randomUUID()` (36 chars, matches `^[A-Za-z0-9_-]{8,64}$`) |
| Retry after network error, timeout or HTTP 503 | **the same key and the same body** |
| User presses Submit again without changing anything after such a failure | the same key (treated as a retry) |
| User changed a field | new key (it is a different request) |
| Definite answer (201, 4xx, 422 recorded as FAILED) | key forgotten; next submit is new |

Why: after a timeout the client cannot know whether the transfer committed. Re-sending the same key makes the backend replay the stored result (`Idempotent-Replayed: true`) instead of moving money twice (FR-TRF-05, FR-CON-08). While a request is in flight the button is disabled **and** a ref guard ignores further calls, so a double click cannot send two requests (UI-05).

A 5xx response without the backend's `{"code": ...}` error body (for example the dev proxy answering 502 because the backend is down) is treated as a network error and is retryable. A real backend 500 always carries `INTERNAL_ERROR` and means the transaction was rolled back.

## 3. Sessions and roles

- The login response carries the token, role, account ID and `displayName` (decision D3, for the top bar). The JWT itself keeps only ID, role and expiry.
- The session is stored in `sessionStorage`: it survives a refresh and disappears with the tab. Any 401 on a request that carried a token, or the 60-minute expiry, logs the user out with "Your session has expired". Known limitation: a script injected into the page could read `sessionStorage`. Mitigations: React escapes output, there are no third-party scripts and tokens last 60 minutes. An httpOnly cookie would need backend changes and is out of scope.
- Role-based screens (UI-03) are a convenience only. The backend still checks role and ownership on every request (FR-AUTH-08/09, [[api-error-contract-and-access-control]]).

## 4. No CORS: same-origin through a proxy

The backend has no CORS configuration. The Vite dev and preview servers forward `/api` to `http://localhost:8080`, so the browser only ever talks to its own origin. No backend change was needed.

## Sources

- SRS v1.2 section 3.1 and UI-01..UI-05; SRS 3.4 (headers, error body)
- [[api-idempotency-and-retry-semantics]], [[monetary-representation-and-integer-units]], [[api-error-contract-and-access-control]]
- IETF draft "The Idempotency-Key HTTP Header Field" (SRS reference R7)
