# ADR-006: Stateless Authentication via JWT and Role-Based Authorization

## Status
Accepted

## Date
2026-11-26

## Context
The system serves three user classes with distinct operational boundaries:
- `USER`: Regular customer holding currency wallets, sending/receiving peer-to-peer transfers, and paying merchants.
- `MERCHANT`: Business account receiving payments and inspecting payment statistics. Cannot initiate outbound transfers.
- `ADMIN`: Platform operator managing test funding, reversals, and reconciliation. Cannot directly modify balances.

We evaluated:
1. **Server-Side Session State**: Storing sessions in memory or database cookies.
2. **Stateless JSON Web Tokens (JWT)**: Cryptographically signed tokens passed via `Authorization: Bearer <token>`.

## Decision
We implement **stateless JWT authentication with server-side Role-Based Access Control (RBAC)**:
1. Successful authentication (`POST /api/v1/auth/login`) issues a signed JWT containing account ID, email, role (`USER`, `MERCHANT`, `ADMIN`), and standard claims.
2. Tokens are signed using HMAC-SHA256 with a 256-bit secret loaded strictly from an environment variable (`JWT_SECRET`).
3. Tokens expire after 60 minutes.
4. Endpoint authorization is enforced at the server level on every request according to the endpoint matrix in SRS Table 3-1.
5. In addition to role checks, resource ownership checks ensure that users can only inspect their own wallets and transactions; unowned resource requests return HTTP 404 `NOT_FOUND`.

## Consequences
- **Positive**:
  - Stateless design eliminates server-side session memory consumption and synchronization.
  - Clear separation of privileges between Users, Merchants, and Admins.
  - Secrets are isolated from version control.
- **Negative**:
  - Tokens cannot be easily revoked prior to expiration without maintaining a distributed token blacklist (mitigated by short 60-minute lifetime).

## Citations
- Second Brain Reference: [[api-security-and-input-validation]]
- SRS Traceability: Section 2.3, Table 3-1, FR-AUTH-01 to FR-AUTH-06, NFR-SEC-01 to NFR-SEC-03
