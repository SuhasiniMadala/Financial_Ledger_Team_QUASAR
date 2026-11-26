# JWT Authentication and Role-Based Authorization

## Summary

Securing financial REST APIs requires robust identity verification (authentication) and fine-grained access control (authorization).

JSON Web Tokens (JWT) provide stateless authentication tokens carrying verified claims and signatures.

Role-Based Access Control (RBAC) restricts API endpoint access according to user privileges.

## Security Architecture

1. **Token Structure**: A JWT contains a header, payload (account ID, roles, expiration, issued-at), and a cryptographic signature (HMAC-SHA256).
2. **Secret Management**: Tokens are signed using a minimum 256-bit secret stored in an environment variable, never hardcoded in source control.
3. **Role Segregation**:
   - `USER`: Initiates transfers, pays merchants, and manages personal wallets.
   - `MERCHANT`: Receives payments and views payment histories; cannot initiate transfers.
   - `ADMIN`: Runs reconciliation, manages test funding, and views system audits; cannot edit balances directly.
4. **Data Isolation & Ownership**: In addition to role checks, the server enforces account ownership checks. Users can never view or manipulate another user's private financial records.

## Relevance to Our Project

SRS Section 4.1, Table 3-1, and NFR-SEC-01 to NFR-SEC-03 govern authentication, authorization, and secret management.

Endpoints reject unauthorized role access with HTTP 403 `FORBIDDEN` and unowned resources with HTTP 404 `NOT_FOUND`.

## Related Notes

- [[rest-api-security-and-testing]]
- [[idempotency-in-rest-apis]]
- [[double-entry-bookkeeping]]

## Sources

- [RFC 7519 – JSON Web Token (JWT)](https://datatracker.ietf.org/doc/html/rfc7519)
- [NIST – Guide to Role Based Access Control (SP 800-162)](https://csrc.nist.gov/publications/detail/sp/800-162/final)
- [Project Statement: Financial Ledger & Digital Wallet API](../Financial_Wallet_Ledger_Project.pdf)
