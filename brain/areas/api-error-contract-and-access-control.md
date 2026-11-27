---
title: "Standing rules: API error contract and access control"
type: area
authors: [Akshaya A]
created: 2026-11-27
tags: [api, security, errors]
requirements: [FR-AUTH-06, FR-AUTH-07, FR-AUTH-08, FR-AUTH-09, NFR-SEC-03, NFR-SEC-04, NFR-SEC-06, NFR-REL-01]
---

# Standing rules: API error contract and access control

An ongoing responsibility (PARA "area"): every new endpoint must follow these rules, and `SecurityAccessMatrixTest` / `ErrorHandlingAndInputSafetyTest` must be extended when an endpoint is added. Based on [[api-security-and-input-validation]] and [[jwt-authentication-and-role-based-authorization]].

## Error contract

- Every error body has exactly `code, message, timestamp, path, requestId` (and `transactionId` when a FAILED transaction was recorded). `requestId` equals the `X-Request-Id` response header, so a user's screenshot can be matched to the server log.
- Codes come only from SRS Table 3-2 (`ErrorCode.java`). Messages never contain stack traces, SQL, class names or exception text; unexpected errors are logged server-side and answered with a generic `INTERNAL_ERROR`.
- Malformed JSON, unknown fields, fractional or string amounts all become `400 VALIDATION_ERROR` (Jackson coercion is switched off in `application.properties`).

## Access control

- Role rules live in one place: `SecurityConfig` mirrors Table 3-1.
- Ownership is checked in services, and "not yours" is always answered exactly like "does not exist": **404 with the same message** (FR-AUTH-09). Never 403 for someone else's wallet or transaction - that would reveal it exists.
- Login failures are one generic 401 for unknown email and wrong password, and both paths run a BCrypt comparison so timing does not reveal which emails exist.
- No secrets in the repository: the JWT secret comes from `LEDGER_JWT_SECRET` (>= 32 bytes, checked at start-up); tests and local-check use a random per-process key.

## Checklist for a new endpoint

1. Add it to `SecurityConfig` and to the `TABLE_3_1` list in `SecurityAccessMatrixTest`.
2. Validate every parameter (`Ids.require`, `PageQuery.of`, `DateRange.parse`, bean validation).
3. Add an API test for its 400/401/403/404 cases.
4. Check that `OpenApiDocumentationTest` still passes.
