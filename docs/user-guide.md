# User Guide (per role)

SRS 2.6 asks for a short guide for each role. Part A uses the React client (Stage 4). Part B shows the same tasks directly against the API with Swagger UI (`/swagger-ui.html`), Postman (`docs/api/financial-ledger.postman_collection.json`) or curl.

All money is simulated. Only an admin's test funding adds money.

## Part A - Using the React client

Start the backend and the client (README sections 5 and 5a), then open http://localhost:5173. The top bar shows the product name, your name and role, and **Logout**. You only see the screens of your own role. Sessions end after 60 minutes ("Your session has expired": log in again).

Amounts are typed in rupees, e.g. `250` or `250.50` (at most two decimals) and shown as `₹1,00,000.00`. Every money form disables its button while sending. If the network fails, the server does not answer, or it is busy, press **Retry (same request)**: it re-sends the same request with the same safety key, so you are **never charged twice**. If you change any field first, it becomes a new request.

### USER

| Screen | What you can do |
|---|---|
| Register / Log in | Create an account (name, email, password of 8-72 characters) and log in. |
| Dashboard | See your wallets, balances and status. Create a wallet (one per currency, INR or USD; it starts at ₹0.00). **Copy ID** to give someone your wallet ID so they can send you money. |
| Transfer | Choose your wallet, paste the other user's wallet ID (`W-` + 10 characters), enter the amount and an optional description (140 characters). Same currency only. |
| Pay Merchant | Choose your wallet, enter the merchant code (`MER-` + 6 characters), the amount and an optional reference (64 characters). |
| History | All your transactions and your own failed attempts, newest first, with filters for wallet, type, status and dates. Open one to see its details, your own ledger entry and any reversal. |

If a transfer is rejected (e.g. not enough money, frozen wallet), the message says why, nothing moves, and the failed attempt is kept in your history.

### MERCHANT

| Screen | What you can do |
|---|---|
| Register / Log in | Register as a merchant with your business name; you get a merchant code and an INR wallet at ₹0.00. |
| Account | Business name, merchant code (give it to customers), wallet, balance and status. Merchants cannot send money. |
| Payments | Payments received and refunds (reversals), newest first, with payer name and reference, filtered by date. Payers' wallets and balances are never shown. |
| Statistics | Number, total and average of completed payments for a date range (default: the last 30 days). |

### ADMIN

The admin account comes only from the server's configuration (`LEDGER_ADMIN_EMAIL`, `LEDGER_ADMIN_PASSWORD`); it cannot be registered.

| Screen | What you can do |
|---|---|
| Users & Merchants | Everyone registered, with email, registration time and merchant codes. |
| Wallets | Every account including the System Funding account. **Freeze / Unfreeze / Deactivate** with a required reason (deactivation is final). **Test funding** adds simulated money to an ACTIVE user wallet. **Entries** shows an account's ledger. Nobody can edit a balance directly. |
| Transactions & Ledger | All transactions with filters. Open one to see its debit and credit entries (always balanced) and to **Reverse** it with a 10-500 character reason. A transaction can be reversed once; failed transactions and reversals cannot be reversed. |
| Reconciliation | Run the read-only check: debits = credits per currency, every completed transaction balances, every stored balance equals the ledger. Shows PASS or the discrepancies. Nothing is changed. |
| Statistics | Users, merchants, wallets by status, completed and failed transactions, totals by type and the latest reconciliation result. |

## Part B - Using the API directly

Amounts are always integers in paise: Rs 250.00 = `25000`.

Every money-moving request needs an **Idempotency-Key** header: a new random value (8-64 letters, digits, `-`, `_`) for each new payment. If the network fails or you get **503**, send the *same* request with the *same* key again - you will never be charged twice. A replayed answer carries `Idempotent-Replayed: true`.

## USER

1. **Register**: `POST /api/v1/auth/register` `{"displayName":"Asha Rao","email":"asha@example.test","password":"at-least-8-chars"}`.
2. **Log in**: `POST /api/v1/auth/login` -> copy `accessToken`; send it as `Authorization: Bearer <token>`. Tokens last 60 minutes.
3. **Create a wallet**: `POST /api/v1/wallets` `{"currency":"INR"}` (one per currency; `INR` and `USD` are supported). It starts at Rs 0.00. Ask the admin for test funding.
4. **See wallets and balances**: `GET /api/v1/wallets`.
5. **Send money to another user**: `POST /api/v1/transfers` with header `Idempotency-Key` and body `{"sourceWalletId":"W-...","destinationWalletId":"W-...","amountMinor":100000,"description":"rent share"}`. You need the receiver's wallet ID. Same currency only; maximum Rs 1,00,000.00 per transfer.
6. **Pay a merchant**: `POST /api/v1/payments` with `Idempotency-Key` and `{"sourceWalletId":"W-...","merchantCode":"MER-7Q2K9D","amountMinor":25000,"reference":"Order 1187"}`.
7. **History**: `GET /api/v1/transactions` (filters `walletId`, `type`, `status`, `from`, `to` as yyyy-MM-dd; `page`, `size`). Details: `GET /api/v1/transactions/{id}`. You see money in and out, and your own failed attempts.

Common answers: `422 INSUFFICIENT_FUNDS` (not enough money), `422 WALLET_NOT_ACTIVE` (a wallet is frozen or closed), `422 CURRENCY_MISMATCH`, `422 INVALID_DESTINATION` (not a user wallet / unknown merchant), `422 SAME_WALLET`, `409 IDEMPOTENCY_KEY_REUSED` (you reused a key for a different request - use a new key). A rejected attempt is still listed in your history as FAILED.

## MERCHANT

1. **Register**: `POST /api/v1/auth/register/merchant` `{"businessName":"Campus Cafe","email":"...","password":"..."}`. You receive a **merchant code** (e.g. `MER-7Q2K9D`) and an INR wallet - give the code to customers.
2. **Log in** as above.
3. **Account**: `GET /api/v1/merchant/account` (name, code, wallet, balance, status).
4. **Payments received**: `GET /api/v1/merchant/transactions?from=2026-11-01&to=2026-11-30` - time, amount, payer name and reference. You never see a payer's wallet or balance.
5. **Statistics**: `GET /api/v1/merchant/statistics` (default last 30 days): count, total, average.

Merchants cannot send money (`403` on transfers and payments).

## ADMIN

The admin account is created from the server's configuration (`LEDGER_ADMIN_EMAIL`, `LEDGER_ADMIN_PASSWORD`); it cannot be registered through the API.

1. **Directory**: `GET /api/v1/admin/users`, `/admin/merchants`, `/admin/accounts` (includes the System Funding Accounts).
2. **Add test money**: `POST /api/v1/admin/wallets/{walletId}/test-funding` with `Idempotency-Key` and `{"amountMinor":500000}` - only to ACTIVE user wallets.
3. **Freeze / unfreeze / deactivate** a wallet: `POST /api/v1/admin/wallets/{id}/freeze` `{"reason":"reported lost device"}`. Deactivation is final. Balances never change.
4. **Correct a mistake**: `POST /api/v1/admin/transactions/{id}/reversal` with `Idempotency-Key` and `{"reason":"sent to wrong recipient"}` (10-500 characters). The original stays unchanged; a REVERSAL with mirror-image entries is added. A transaction can be reversed once.
5. **Inspect**: `GET /api/v1/admin/transactions` (filters), `/admin/transactions/{id}` (with entries), `/admin/accounts/{id}/entries`, `/admin/statistics`.
6. **Reconcile**: `POST /api/v1/admin/reconciliations` - `PASS` means debits = credits per currency, every completed transaction balances, and every stored balance equals the ledger. `FAIL` lists each discrepancy with expected, actual and difference. Reconciliation never changes data.
