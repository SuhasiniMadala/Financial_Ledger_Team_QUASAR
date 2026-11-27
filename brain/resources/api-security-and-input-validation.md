---
title: "API Security and Input Validation"
type: resource
authors: [Akshaya A]
created: 2026-11-26
---

# API Security and Input Validation

## Summary

Financial APIs handle sensitive economic resources and are frequent targets for fraud, manipulation, and unauthorized balance access. Security requires defense-in-depth: stateless authentication, fine-grained role-based access control (RBAC), resource ownership verification, rigorous input validation, and parameter bounds checking.

## Security Controls Architecture

1. **Authentication (JWT)**:
   - Users authenticate with email and BCrypt-hashed password ($\ge 10$ work factor).
   - Server issues an HMAC-SHA256 signed JSON Web Token (JWT) with 60-minute expiry.
   - Secret key ($\ge 256$ bits) is supplied exclusively via environment variables, never committed to source control.
2. **Role-Based Authorization**:
   - `USER`: May create wallets, transfer funds, pay merchants, view own wallets and transactions.
   - `MERCHANT`: May view business profile, receive payments, view payment history and statistics; **cannot initiate transfers**.
   - `ADMIN`: Provisioned via deployment configuration only; may freeze/unfreeze wallets, credit test funding, execute reversals, and run reconciliation; **cannot edit balances directly**.
3. **Resource Ownership Isolation**:
   - If User A attempts to query User B's wallet or transaction details, the API returns `404 NOT_FOUND` rather than `403 FORBIDDEN` to prevent resource enumeration.
4. **Input Validation & Negative Amount Prevention**:
   - Validate all IDs, amounts, currencies, and headers against strict constraints.
   - Reject non-positive amounts ($A \le 0$) to eliminate balance inflation attacks.
   - Parameterized SQL statements eliminate SQL injection vulnerabilities.

## Relevance to Project Requirements

- **NFR-SEC-01 to NFR-SEC-06**: Password hashing, JWT signing, server-side authentication, input validation, SQL injection prevention, and stack trace suppression.
- **FR-AUTH-01 to FR-AUTH-06**: User registration, merchant codes, admin provisioning, and role protection.

## Related Notes

- [[api-idempotency-and-retry-semantics]]
- [[monetary-representation-and-integer-units]]
- [[double-entry-accounting-principles]]

## Sources

- OWASP API Security Top 10 (https://owasp.org/API-Security/)
- RFC 7519: *JSON Web Token (JWT)*
- Financial Ledger SRS v1.4, Section 4.1 & Section 5.3
