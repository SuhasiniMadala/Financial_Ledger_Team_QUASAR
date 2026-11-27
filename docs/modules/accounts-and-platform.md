# Module: Accounts, Wallets and Platform (Member 1 - Akshaya A)

Issues: #5, #6, #7, #8. Requirements: FR-AUTH-01..09, FR-WAL-01..06, FR-ADM-01/02/04-06, NFR-REL-01/02, NFR-SEC-01..06, NFR-MAIN-01/03, DB-01..04.

## What it contains

| Package | Responsibility |
|---|---|
| `config/` | `LedgerProperties` (validated settings), `DatabaseConfig` (SQLite WAL, FULL sync, FKs, busy timeout, pool), `SqliteImmediateTransactionManager` (ADR-009), `SecurityConfig` (Table 3-1 roles), `JwtAuthenticationFilter`, `SecurityErrorHandler`, `OpenApiConfig`, `PlatformConfig` (Clock, BCrypt) |
| `common/` | `ErrorCode` (Table 3-2), `ErrorResponse`, `GlobalExceptionHandler` (503 on SQLITE_BUSY, no internals leaked), `RequestIdFilter`, `IdGenerator` (provider interface, NFR-MAIN-03), `MoneyFormatter` (Indian grouping, integer only), `PageQuery`, `DateRange` |
| `module/auth` | registration (user, merchant + code + INR wallet), login (generic 401), JWT (HS256, >= 256-bit secret from env, 60 min), configured admin bootstrap |
| `module/wallet` | wallet creation (one per currency, supported currencies only), owner-scoped reads (404 if not yours), admin status changes with audit log, System Funding Accounts at start-up |
| `module/admin` (directory part) | users, merchants, accounts lists; freeze/unfreeze/deactivate |
| `module/health` | `GET /api/v1/health` - UP only if a database read succeeds |

## Key rules (see [[api-error-contract-and-access-control]])

- Another party's wallet or transaction always answers **404** with the same message as a missing one.
- Passwords 8-72 characters and at most 72 bytes; BCrypt work factor 10.
- Unknown JSON fields, fractional amounts and strings for numbers are rejected (400).

## Tests

`AuthApiTest` (11), `WalletApiTest` (6), `AdminDirectoryApiTest` (7), `HealthApiTest` (2), `SecurityAccessMatrixTest` (all 27 Table 3-1 endpoints x 4 callers), `ErrorHandlingAndInputSafetyTest` (4), `DatabaseConfigurationTest` (4), `ArchitectureRulesTest` (3), `JwtServiceTest` (4), `MoneyFormatterTest` (11), `IdGeneratorTest` (3).

## Defects in this area

DEF-001 (test config shadowing), DEF-003 (OpenAPI config), DEF-004/005 (local-check) - see `docs/testing/defect-log.md`.
