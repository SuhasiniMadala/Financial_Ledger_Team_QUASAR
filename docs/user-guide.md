# User Guide (per role)

SRS 2.6 asks for a short guide for each role. Until the React client arrives (Stage 4), use Swagger UI (`/swagger-ui.html`), Postman (`docs/api/financial-ledger.postman_collection.json`) or curl. Amounts are always integers in paise: Rs 250.00 = `25000`.

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
