# Stage 4 API Contracts (backend as built, and the shared frontend contracts)

Part A is read from the backend source on `main` @ `b1be847` (tag `v0.3.0-stage3`), not from the README. Part B is the frontend contract that **must exist before parallel work starts**. Akshaya builds it in the foundation PR. Everyone else only *uses* it.

If the backend and this file disagree, the backend wins and this file must be fixed in a PR.

---

## Part A - Backend contract (verified in code)

### A.1 General rules

| Rule | Value | Where in code |
|---|---|---|
| Base path | `/api/v1` | every `@RequestMapping` / `@GetMapping` |
| Auth header | `Authorization: Bearer <accessToken>` | `config/SecurityConfig.java` |
| Token contents | `sub` = account ID, `role` claim only. **No display name, no email.** | `module/auth/service/JwtService.java` |
| Token lifetime | 60 minutes (`expiresInSeconds` = 3600 in the login response) | `application.properties` `ledger.jwt.expiry-minutes` |
| Roles | `USER`, `MERCHANT`, `ADMIN` (returned as `role` in login) | `SecurityConfig` `hasRole(...)` |
| Role enforcement | `/admin/**` ADMIN, `/merchant/**` MERCHANT, `/wallets/**`, `/transfers`, `/payments`, `/transactions/**` USER | `SecurityConfig.java` lines 39-42 |
| CORS | **Not configured.** The browser must reach the API on the same origin, so the frontend uses the Vite dev/preview proxy (`/api` -> `http://localhost:8080`). | `SecurityConfig.java` |
| Money in JSON | integer paise in `...Minor` fields (`amountMinor`, `balanceMinor`). Floats in requests are rejected (`accept-float-as-int=false`). | `application.properties` |
| Formatted money | Most responses also include `...Formatted` (e.g. `"₹1,00,000.00"`), made by `common/util/MoneyFormatter.java` | DTOs below |
| Unknown JSON fields | rejected with 400 (`fail-on-unknown-properties=true`) - **never send extra fields** | `application.properties` |
| Null fields | omitted from responses (`default-property-inclusion=non_null`) - treat every optional field as possibly missing | `application.properties` |
| Paging | query `page` (0-based, default 0) and `size` (1-100, default 20). Response `PageResponse<T>` = `{items, page, size, totalItems, totalPages}` | `common/api/PageQuery.java`, `PageResponse.java` |
| Date filters | `from`, `to` as `yyyy-MM-dd`, inclusive, UTC | `common/util/DateRange.java` |
| Timestamps | ISO-8601 UTC strings (`createdAt`, `runAt`, ...) | DTOs |
| Per-transaction limit | 1 .. 10,000,000 paise (Rs 1,00,000.00) | `ledger.max-amount-minor`, ADR-008 |
| Currencies | `INR`, `USD` (`ledger.currencies`). Merchants have one INR wallet. | ADR-008 |

### A.2 ID formats (`common/id/Ids.java`)

| ID | Regex | Example |
|---|---|---|
| Wallet | `^W-[0-9A-Z]{10}$` | `W-7K2M9Q4X1B` |
| System funding account (admin views only) | `^SYS-FUND-[A-Z]{3}$` | `SYS-FUND-INR` |
| Transaction | `^T-[0-9A-Z]{12}$` | `T-0A1B2C3D4E5F` |
| Merchant code | `^MER-[0-9A-Z]{6}$` | `MER-7Q2K9D` |
| Idempotency key | `^[A-Za-z0-9_-]{8,64}$` (`IdempotencyKeys.java`) - a standard UUID from `crypto.randomUUID()` fits | |

### A.3 Error body (every 4xx/5xx) - `common/api/ErrorResponse.java`

```json
{ "code": "INSUFFICIENT_FUNDS", "message": "The source wallet balance is too low",
  "timestamp": "2026-11-28T10:00:00Z", "path": "/api/v1/transfers", "requestId": "...",
  "transactionId": "T-..." }
```

`transactionId` is present only when a FAILED transaction was recorded (422 on transfers and payments). Error codes (`common/api/ErrorCode.java`):

| HTTP | Codes |
|---|---|
| 400 | `VALIDATION_ERROR`, `IDEMPOTENCY_KEY_REQUIRED` |
| 401 | `UNAUTHENTICATED` (bad login, missing/expired token) |
| 403 | `FORBIDDEN` (wrong role) |
| 404 | `NOT_FOUND` (missing **or not yours**) |
| 409 | `DUPLICATE_RESOURCE`, `IDEMPOTENCY_KEY_REUSED`, `ALREADY_REVERSED`, `INVALID_STATE_TRANSITION` |
| 422 | `INSUFFICIENT_FUNDS`, `WALLET_NOT_ACTIVE`, `CURRENCY_MISMATCH`, `SAME_WALLET`, `INVALID_DESTINATION`, `NOT_REVERSIBLE` |
| 500 | `INTERNAL_ERROR` (rolled back) |
| 503 | `SERVICE_BUSY` + header `Retry-After: 1` (safe to retry with the **same** key) |

Idempotent endpoints (marked ★) also send `Idempotent-Replayed: true` when the stored 201 or 422 is replayed.

### A.4 Endpoints, requests and responses

**Public** (`module/auth/controller/AuthController.java`, `module/health/HealthController.java`)

| Method + path | Request body | Success | Response body |
|---|---|---|---|
| `POST /auth/register` | `{displayName (1-100), email, password (8-72)}` | 201 | `RegistrationResponse {accountId, role, email, displayName, createdAt}` |
| `POST /auth/register/merchant` | `{businessName (1-100), email, password (8-72)}` | 201 | `RegistrationResponse {..., merchantCode, walletId}` |
| `POST /auth/login` | `{email, password}` | 200 | `LoginResponse {accessToken, tokenType, expiresInSeconds, accountId, role}` - **no displayName (see gap G-1)** |
| `GET /health` | - | 200 | `{status, database}` |

**USER** (`module/wallet/controller/WalletController.java`, `module/transfer/controller/*`)

| Method + path | Request | Success | Response |
|---|---|---|---|
| `POST /wallets` | `{currency: "INR" or "USD"}` | 201 | `WalletResponse {walletId, currency, balanceMinor, balanceFormatted, status, createdAt}` |
| `GET /wallets` | - | 200 | `WalletResponse[]` (**plain array, not paged**) |
| `GET /wallets/{id}` | - | 200 | `WalletResponse` (404 if not yours) |
| `POST /transfers` ★ | header `Idempotency-Key`; `{sourceWalletId, destinationWalletId, amountMinor, description? (<=140)}` | 201 | `TransactionReceipt {transactionId, type, status, amountMinor, amountFormatted, currency, sourceAccountId, destinationAccountId, reversalOfId?, description?, createdAt}` |
| `POST /payments` ★ | header `Idempotency-Key`; `{sourceWalletId, merchantCode, amountMinor, reference? (<=64)}` | 201 | `TransactionReceipt` |
| `GET /transactions` | query `walletId?, type?, status?, from?, to?, page?, size?` | 200 | `PageResponse<UserTransactionView>` |
| `GET /transactions/{id}` | - | 200 | `UserTransactionDetail {transaction: UserTransactionView, ownEntries: LedgerEntryView[]}` |

`UserTransactionView = {transactionId, type, status, direction (OUTGOING/INCOMING), walletId, amountMinor, amountFormatted, currency, counterpartyName?, description?, failureCode?, reversalOfId?, reversedBy?, createdAt}`. FAILED attempts appear only for the user who made them.

`type` values: `TEST_FUNDING`, `TRANSFER`, `MERCHANT_PAYMENT`, `REVERSAL`. `status` values: `COMPLETED`, `FAILED`.

**MERCHANT** (`module/transfer/controller/MerchantController.java`)

| Method + path | Query | Response |
|---|---|---|
| `GET /merchant/account` | - | `MerchantAccountResponse {businessName, merchantCode, walletId, currency, balanceMinor, balanceFormatted, status}` |
| `GET /merchant/transactions` | `from?, to?, page?, size?` | `PageResponse<MerchantTransactionView {transactionId, type, direction, amountMinor, amountFormatted, currency, payerName?, reference?, reversalOfId?, createdAt}>` - no payer wallet ID or balance by design |
| `GET /merchant/statistics` | `from?, to?` (default: last 30 days) | `MerchantStatisticsResponse {from, to, currency, paymentCount, totalMinor, totalFormatted, averageMinor, averageFormatted}` |

Note: a reversed payment stays `COMPLETED` and is still counted in statistics; the refund is a separate `REVERSAL` row in the list (`PaymentAndMerchantApiTest.statisticsCountReversedPaymentsAsCompleted`).

**ADMIN** (`module/admin/controller/*`, `module/reconciliation/controller/ReconciliationController.java`)

| Method + path | Request / query | Response |
|---|---|---|
| `GET /admin/users` | `page?, size?` | `PageResponse<UserSummaryResponse {accountId, name, email, registeredAt, merchantCode?}>` |
| `GET /admin/merchants` | `page?, size?` | same type, `merchantCode` set |
| `GET /admin/accounts` | `page?, size?` | `PageResponse<AccountSummaryResponse {accountId, ownerId, ownerName, type, currency, balanceMinor, balanceFormatted, status, createdAt}>` (includes `SYS-FUND-*`) |
| `POST /admin/wallets/{id}/freeze` / `unfreeze` / `deactivate` | `{reason (1-500)}` | `WalletResponse`; 409 `INVALID_STATE_TRANSITION` for an illegal change; deactivate is final |
| `POST /admin/wallets/{id}/test-funding` ★ | header key; `{amountMinor, description? (<=140)}` | 201 `TransactionReceipt` (ACTIVE user wallets only) |
| `GET /admin/transactions` | `type?, status?, walletId?, from?, to?, page?, size?` | `PageResponse<TransactionView>` |
| `GET /admin/transactions/{id}` | - | `TransactionDetailResponse {transaction: TransactionView, entries: LedgerEntryView[]}` |
| `POST /admin/transactions/{id}/reversal` ★ | header key; `{reason (10-500)}` | 201 `TransactionReceipt`; 409 `ALREADY_REVERSED`; 422 `NOT_REVERSIBLE` / `INSUFFICIENT_FUNDS` / `WALLET_NOT_ACTIVE` |
| `GET /admin/accounts/{id}/entries` | `page?, size?` | `PageResponse<LedgerEntryView {entryId, sequenceNo, transactionId, accountId, direction, amountMinor, balanceAfterMinor, createdAt}>` |
| `GET /admin/statistics` | - | `StatisticsResponse {users, merchants, walletsByStatus{}, transactionsByStatus{}, completedTotals[{type, currency, count, totalMinor, totalFormatted}], latestReconciliation?{runId, runAt, result, discrepancyCount}}` |
| `POST /admin/reconciliations` | - (no body, no key) | `ReconciliationReport {runId, runAt, durationMs, result (PASS/FAIL), currencies[{currency, totalDebitsMinor, totalCreditsMinor, ...Formatted}], checked{accounts, transactions, entries}, discrepancies[{type, subjectId, expectedMinor, actualMinor, differenceMinor, detail}]}` |

`TransactionView = {transactionId, type, status, amountMinor, amountFormatted, currency, initiatedBy, sourceAccountId?, destinationAccountId?, reversalOfId?, reversedBy?, failureCode?, description?, createdAt}`.

### A.5 Gaps found in the audit

| ID | Gap | Decision (needs team approval in the plan PR) |
|---|---|---|
| G-1 | SRS 3.1 needs the "account name" in the top bar, but `LoginResponse` and the JWT carry no name. | Akshaya adds `displayName` to `LoginResponse` (one field, auth module, she owns it) and updates `AuthApiTest`, `docs/api/openapi.json` and the Postman collection. Fallback if the team rejects a backend change: show the email typed at login (merchants: `businessName` from `/merchant/account`). |
| G-2 | No CORS. | No backend change. The frontend calls relative `/api/...` and Vite proxies to `http://localhost:8080` in `dev` and `preview`. Recorded in ADR-011. |
| G-3 | No user directory for USERs (by design: privacy). A sender must know the recipient's wallet ID. | Dashboard shows each wallet ID with a "Copy" button; the transfer form validates the `W-` format. No new endpoint. |

---

## Part B - Shared frontend contracts (built by Akshaya in PR A1, used by everyone)

Stack: React (the version `npm create vite@latest -- --template react-ts` installs) + TypeScript + Vite, `react-router-dom`, Vitest + React Testing Library + jsdom, ESLint (typescript-eslint + react-hooks). **No** UI kit, **no** axios, **no** uuid package (`crypto.randomUUID()`), **no** money library. Node 20 LTS or newer.

### B.1 Folder layout (`frontend/`)

```text
frontend/
  package.json, package-lock.json, vite.config.ts, tsconfig.json, eslint.config.js, index.html   (Akshaya)
  src/main.tsx, src/App.tsx, src/styles/global.css                                               (Akshaya)
  src/api/client.ts, src/api/errors.ts, src/api/types.ts                                         (Akshaya)
  src/auth/AuthContext.tsx, src/auth/session.ts, src/auth/RequireRole.tsx                       (Akshaya)
  src/lib/money.ts, src/lib/errorMessages.ts, src/lib/dates.ts                                   (Akshaya)
  src/hooks/useFinancialSubmit.ts, src/hooks/usePagedQuery.ts                                   (Akshaya)
  src/components/*  (Layout, TopBar, NavBar, Money, SubmitButton, ErrorBanner, Loading,
                     EmptyState, Pagination, DateRangeFilter, ConfirmDialog, CopyButton)        (Akshaya)
  src/pages/LoginPage.tsx, RegisterPage.tsx, RegisterMerchantPage.tsx, NotFoundPage.tsx, ForbiddenPage.tsx (Akshaya)
  src/test/renderWithProviders.tsx, src/test/fakeClient.ts                                      (Akshaya)
  src/features/user-money/**     Dashboard, Transfer, Pay Merchant                               (Akshaya)
  src/features/user-history/**   History, Transaction detail                                     (Suhasini)
  src/features/merchant/**       Account, Payments, Statistics                                   (Suhasini)
  src/features/admin/**          Directory, Wallets, Transactions & Ledger, Reconciliation, Statistics (Geethika)
  tests/integration/setup.ts, tests/integration/helpers.ts, vitest.integration.config.ts        (Suhasini)
  tests/integration/<feature>.int.test.ts                                                        (feature owner)
```

### B.2 API client - `src/api/client.ts`

```ts
export type Query = Record<string, string | number | undefined>;

export interface FinancialResult<T> { data: T; replayed: boolean }   // replayed = header Idempotent-Replayed: true

export interface ApiClient {
  get<T>(path: string, query?: Query): Promise<T>;                   // path relative to /api/v1, e.g. '/wallets'
  post<T>(path: string, body?: unknown): Promise<T>;                 // non-financial POSTs (register, status change, reconciliation)
  postFinancial<T>(path: string, body: unknown, idempotencyKey: string): Promise<FinancialResult<T>>;
}

export function createApiClient(options: {
  baseUrl?: string;                 // '' in the browser (Vite proxy); 'http://localhost:8080' in integration tests
  getToken?: () => string | null;
  onUnauthorized?: () => void;      // called on any 401 -> AuthContext logs out and shows "session expired"
  timeoutMs?: number;               // default 15000; uses AbortController
}): ApiClient;
```

- Every failure throws `ApiError` (`src/api/errors.ts`):
  `class ApiError extends Error { kind: 'http' | 'network' | 'timeout'; status?: number; code?: string; transactionId?: string; replayed: boolean; body?: ApiErrorBody }`.
- `isRetryable(err)` returns true for `kind === 'network' | 'timeout'` or `status === 503`.
- A response whose body is **not** our `ErrorResponse` JSON (no `code` field) and whose status is 5xx counts as `kind: 'network'`. Example: the Vite proxy answers 500/502/504 by itself when the backend is down. A real backend 500 always has `code: "INTERNAL_ERROR"` and stays `kind: 'http'`.
- Components get the session-bound client with `useApiClient()` from `AuthContext`. Feature API modules are plain functions that take the client: `export const listWallets = (c: ApiClient) => c.get<Wallet[]>('/wallets');`. That is what makes them usable from integration tests.

### B.3 Money - `src/lib/money.ts` (UI-01, UI-02, TQ-6)

```ts
export function formatMinor(amountMinor: number, currency: string): string;
// INR: '₹' + Indian grouping + 2 decimals: 10000000 -> '₹1,00,000.00'; USD: '$' + western grouping.
// Pure string/integer arithmetic. Must give the same output as backend MoneyFormatter for the cases in MoneyFormatterTest.

export type ParseResult = { ok: true; paise: number } | { ok: false; reason: 'EMPTY' | 'FORMAT' | 'TOO_MANY_DECIMALS' | 'ZERO' | 'TOO_LARGE' };
export function parseRupeesToPaise(input: string, maxPaise?: number): ParseResult;
// Accepts '1', '1.5', '1.50', '1,00,000.00' (commas stripped). Rejects '1.505', '-1', '1e3', '0', '0.00', ' ', 'abc'.
// Implementation: regex + string split + padEnd + Number(integer string). Never parseFloat, never * 100 on a decimal.
```

`src/components/Money.tsx`: `<Money minor={n} currency="INR" />` renders `formatMinor`. **All amounts on every screen go through `formatMinor` or `<Money>`.**

### B.4 Financial submission - `src/hooks/useFinancialSubmit.ts` (UI-04, UI-05)

```ts
export function useFinancialSubmit<Req, Res>(send: (body: Req, key: string) => Promise<FinancialResult<Res>>): {
  submit: (body: Req) => Promise<void>;   // NEW logical request: new crypto.randomUUID() key, remembers body+key
  retry: () => Promise<void>;             // SAME key + SAME body; only when canRetry
  reset: () => void;                      // forget key (call when the user edits the form)
  inFlight: boolean;                      // disable the submit button while true (UI-05)
  canRetry: boolean;                      // last error was network / timeout / 503
  result?: Res; replayed: boolean; error?: ApiError;
};
```

Rules: a second `submit` while `inFlight` is ignored (ref guard, not only the disabled button). Any change to a form field calls `reset()`, so the next submit gets a new key. `IDEMPOTENCY_KEY_REUSED` must never happen from the UI; if it does, show the error and `reset()`. Used by: Transfer, Pay Merchant (Akshaya), Test funding, Reversal (Geethika).

### B.5 Errors - `src/lib/errorMessages.ts` (UI-03)

`messageFor(err: ApiError): string` has a readable sentence for **all 17 codes** in A.3, plus `network` and `timeout`. For 422 with `transactionId` it appends "Recorded as failed transaction T-...". Nobody adds codes later. If one is missing, ask Akshaya.

### B.6 Auth and routing

- `session.ts`: `Session = { token, accountId, role: 'USER'|'MERCHANT'|'ADMIN', displayName, expiresAt }`, stored in `sessionStorage` (see ADR-011 for why and the XSS limitation). Expired session -> treated as logged out.
- `RequireRole role="ADMIN"` wraps role routes: not logged in -> `/login`; wrong role -> `ForbiddenPage`.
- **Route registry (avoids everyone editing `App.tsx`)**: each feature folder has `routes.tsx` exporting

```ts
export const featureRoutes: { role: 'USER'|'MERCHANT'|'ADMIN'; nav: { path: string; label: string; order: number }[]; routes: { path: string; element: React.ReactElement }[] };
```

  `App.tsx` imports the four registries once. Akshaya's foundation PR creates **placeholder** `routes.tsx` files for `user-history`, `merchant` and `admin` (one "not built yet" page each). Their owners replace only their own file.
- Paths: USER `/user/dashboard`, `/user/transfer`, `/user/pay`, `/user/history`, `/user/history/:id`; MERCHANT `/merchant/account`, `/merchant/payments`, `/merchant/statistics`; ADMIN `/admin/directory`, `/admin/wallets`, `/admin/transactions`, `/admin/transactions/:id`, `/admin/accounts/:id/entries`, `/admin/reconciliation`, `/admin/statistics`.

### B.7 Styling

`src/styles/global.css` (Akshaya) defines layout, colours and the button/table/form classes. Feature screens use CSS Modules in their own folder (`*.module.css`). Nobody else edits `global.css`; ask Akshaya.

### B.8 Changing a shared file

Open a GitHub issue labelled `stage-4` + `shared-change`, assign the owner, describe the change. The owner makes it in a small PR. Never edit someone else's file in your own branch.
