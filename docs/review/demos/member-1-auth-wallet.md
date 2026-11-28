# Demo Guide - Member 1 (Akshaya A): Authentication, Wallets and Admin Wallet Status

Environment: `docs/review/demo-environment.md` (backend + client running, fresh database). Time: about 5 minutes.

Fill **Actual** and **Status** during the rehearsal and again in the live review. Status is PASS only if you saw the expected result yourself; otherwise FAIL or NOT DEMONSTRATED. Never copy tokens or passwords into this file.

## Scenarios

| # | Requirement | Role | Client screen / action | API call (by the client) | Expected | Automated test | Actual | Status |
|---|---|---|---|---|---|---|---|---|
| 1 | FR-AUTH-01 | visitor | Register -> User: "Asha Rao", `asha@example.test` | `POST /api/v1/auth/register` | "Account created for Asha Rao" (201) | `AuthApiTest`, `AuthPages.test.tsx` | | NOT YET RUN |
| 2 | FR-AUTH-01 | visitor | Register -> User: "Ravi Kumar", `ravi@example.test` | same | 201 | same | | NOT YET RUN |
| 3 | FR-AUTH-02, FR-WAL-02 | visitor | Register -> Merchant: "Campus Cafe", `cafe@example.test` | `POST /api/v1/auth/register/merchant` | merchant code `MER-......` and an INR wallet at ₹0.00 shown. **Write down the code.** | `AuthApiTest.registersMerchantWithCodeAndWallet`, `AuthPages.test.tsx` | | NOT YET RUN |
| 4 | FR-AUTH-03 | visitor | Register again with `ASHA@example.test` | `POST /auth/register` | "This already exists..." (409 DUPLICATE_RESOURCE) | `AuthApiTest`, `AuthPages.test.tsx` | | NOT YET RUN |
| 5 | FR-AUTH-04 | visitor | Register with password `short` | none (client check) | "The password must be 8 to 72 characters." | `AuthPages.test.tsx` (backend: `AuthApiTest`) | | NOT YET RUN |
| 6 | FR-AUTH-06 | USER | Log in with a wrong password | `POST /auth/login` | "The email or password is not correct..." (401, same text for an unknown email) | `AuthApiTest.loginSuccessAndGenericFailure` | | NOT YET RUN |
| 7 | FR-AUTH-06, SRS 3.1, D3 | USER | Log in as Asha | `POST /auth/login` | top bar: "Financial Ledger", "Asha Rao USER", Logout; menu: Dashboard, Transfer, Pay Merchant, History | `AuthApiTest.loginReturnsDisplayNameForEveryRole`, `App.test.tsx` | | NOT YET RUN |
| 8 | FR-WAL-01, FR-WAL-04, UI-01 | USER | Dashboard -> Create wallet INR | `POST /wallets`, `GET /wallets` | new `W-..........` wallet, ₹0.00, ACTIVE. **Write down Asha's wallet ID** (Copy ID button) | `WalletApiTest`, `DashboardPage.test.tsx` | | NOT YET RUN |
| 9 | FR-WAL-03 | USER | Create wallet INR again | `POST /wallets` | "...a wallet in this currency" (409) | `WalletApiTest`, `DashboardPage.test.tsx` | | NOT YET RUN |
| 10 | FR-WAL-01 | USER | Log out, log in as Ravi, create an INR wallet. **Write down Ravi's wallet ID.** | same | ₹0.00 ACTIVE | same | | NOT YET RUN |
| 11 | UI-03, FR-AUTH-08 | USER | type http://localhost:5173/admin/directory in the address bar | none | "Not allowed" page (backend would return 403 anyway) | `App.test.tsx`, `SecurityAccessMatrixTest` | | NOT YET RUN |
| 12 | FR-ADM-01 | ADMIN | Log in as admin -> Users & Merchants | `GET /admin/users`, `GET /admin/merchants` | Asha, Ravi; Campus Cafe with its merchant code | `AdminDirectoryApiTest`, `WalletsPage.test.tsx` | | NOT YET RUN |
| 13 | FR-ADM-02 | ADMIN | Wallets | `GET /admin/accounts` | all wallets incl. `SYS-FUND-INR` ("Platform (system)") with type, balance, status | `AdminDirectoryApiTest.listsAccountsIncludingSystemFunding` | | NOT YET RUN |
| 14 | FR-ADM-04/05 | ADMIN | Ravi's wallet -> Freeze, reason "reported lost device" -> Confirm | `POST /admin/wallets/{id}/freeze` | "... is now FROZEN"; buttons become Unfreeze / Deactivate | `AdminDirectoryApiTest`, `WalletsPage.test.tsx` | | NOT YET RUN |
| 15 | FR-ADM-04 | ADMIN | Ravi's wallet -> Unfreeze with a reason | `POST /admin/wallets/{id}/unfreeze` | ACTIVE again (Member 3 needs it ACTIVE) | same | | NOT YET RUN |
| 16 | FR-ADM-06 | ADMIN | point at the screen: no "edit balance" anywhere | - | no operation changes a balance directly | `ArchitectureRulesTest` | | NOT YET RUN |

Optional if time allows: freeze -> deactivate a spare wallet, then show that the only buttons left are none (DEACTIVATED is final, SRS 4.8.2 #2).

## What to say (one sentence each)

- Passwords are BCrypt hashes (NFR-SEC-01); the token is a 60-minute HS256 JWT (NFR-SEC-02); the name in the top bar comes from the new `displayName` field (decision D3).
- The menu only shows your role's screens, but the backend checks every request: hiding a button is not security.
- Someone else's wallet is 404, not 403, so its existence is not revealed (FR-AUTH-09).
