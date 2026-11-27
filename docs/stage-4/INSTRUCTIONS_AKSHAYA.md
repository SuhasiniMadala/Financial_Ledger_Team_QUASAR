# Stage 4 Instructions - Akshaya A (BT2024215, GitHub `Akula-Akshaya`)

You build the **foundation of the React client** that everyone else builds on, plus the **USER money screens**. Your first PR is on the critical path: Geethika and Suhasini cannot start their screens until it is merged. Do A1 first, small and solid, then A2.

Read once: `docs/stage-4/API_CONTRACTS.md` (Part A = backend as built, Part B = the contracts **you** implement). Only commit files listed under "Files you own".

---

## 0. One-time setup (your own computer and your own accounts)

```powershell
git clone https://github.com/SuhasiniMadala/Financial_Ledger_Team_QUASAR.git   # skip if you already have it
cd Financial_Ledger_Team_QUASAR
node -v        # must be v20 or newer; install Node LTS from nodejs.org if not
java -version  # 17 or newer
git config user.name
git config user.email
gh auth status
gh api user --jq .login      # must print: Akula-Akshaya
```

If the name/email are not yours, or `gh` shows another account:

```powershell
git config --global user.name "Akshaya A"
git config --global user.email "<an email that is verified on YOUR GitHub account - see github.com -> Settings -> Emails>"
gh auth login          # GitHub.com -> HTTPS -> Login with a web browser -> sign in as Akula-Akshaya
gh auth setup-git
```

**Shared computer only:** run `gh auth login` to add your account, then `gh auth switch --user Akula-Akshaya` before you push, and set the identity **for this repository only** with `git config user.name "Akshaya A"` and `git config user.email "<your verified email>"` (no `--global`). Check `gh api user --jq .login` and `git config user.email` before every commit. Changing only the display name is not enough. The push credentials and the commit email must both be yours.

---

## PR A1 - Client foundation (branch `stage4/member-1-client-foundation`)

**Why:** SRS 3.1 needs one React client with role-specific screens; UI-01..UI-05 must behave the same on every screen. Building the shared parts once (API client, auth, money, idempotency, layout) stops three people from building three incompatible versions.

**Requirements:** SRS 3.1 (layout, top bar, role navigation), UI-01, UI-02, UI-03, UI-04, UI-05 (shared code), FR-AUTH-01, FR-AUTH-02, FR-AUTH-06, FR-AUTH-07, G-1 (account name), TQ-6.

**Endpoints:** `POST /api/v1/auth/register`, `POST /api/v1/auth/register/merchant`, `POST /api/v1/auth/login` (request/response fields in `API_CONTRACTS.md` A.4).

### Start

```powershell
git switch main
git pull origin main
git switch -c stage4/member-1-client-foundation
```

### Tasks, in order

1. **Scaffold** (from the repository root):
   ```powershell
   npm create vite@latest frontend -- --template react-ts
   cd frontend
   npm install
   npm install react-router-dom
   npm install -D vitest @testing-library/react @testing-library/jest-dom @testing-library/user-event jsdom
   ```
   Delete the template demo content (`src/App.css`, `src/assets/`, the counter in `App.tsx`). In `package.json` set scripts:
   `"dev": "vite"`, `"build": "tsc -b && vite build"`, `"preview": "vite preview"`, `"lint": "eslint ."`, `"typecheck": "tsc -b"` (the template's tsconfig files already set `noEmit`), `"test": "vitest"`, `"test:integration": "vitest run --config vitest.integration.config.ts"` (that config file is created by Suhasini in S1), and `"engines": { "node": ">=20" }`.
2. **`vite.config.ts`**: `server.port = 5173`; `server.proxy` and `preview.proxy` = `{ '/api': 'http://localhost:8080' }`; Vitest `test: { environment: 'jsdom', setupFiles: ['src/test/setup.ts'], exclude: ['tests/integration/**', 'node_modules/**'] }`.
3. **`src/api/`**: `client.ts` (`createApiClient`, `ApiClient`, `FinancialResult`), `errors.ts` (`ApiError`, `isRetryable`, the "non-JSON 5xx = network" rule), `types.ts` (`ApiErrorBody`, `Page<T>`). Exactly as in `API_CONTRACTS.md` B.2.
4. **`src/auth/`**: `session.ts` (save/load/clear in `sessionStorage`, expiry check), `AuthContext.tsx` (`useAuth()`, `useApiClient()`, `login`, `logout`; any 401 -> logout + "Your session has expired, please log in again"), `RequireRole.tsx`.
5. **`src/lib/`**: `money.ts` (`formatMinor`, `parseRupeesToPaise` - B.3), `errorMessages.ts` (all 17 codes + network + timeout - B.5), `dates.ts` (format ISO UTC timestamps for display, `yyyy-MM-dd` helpers for filters).
6. **`src/hooks/`**: `useFinancialSubmit.ts` (B.4), `usePagedQuery.ts` (`{items, page, totalPages, loading, error, setPage, reload}` for any `PageResponse` endpoint).
7. **`src/components/`**: `Layout` + `TopBar` (product name "Financial Ledger", `session.displayName`, Logout) + `NavBar` (built from the route registries for the current role), `Money`, `SubmitButton` (`busy` prop -> disabled + "Sending..."), `ErrorBanner` (uses `messageFor`, optional Retry button when `canRetry`), `Loading`, `EmptyState`, `Pagination`, `DateRangeFilter`, `ConfirmDialog`, `CopyButton`. **`src/styles/global.css`** for the whole app.
8. **`src/pages/`**: `LoginPage` (email + password -> role home), `RegisterPage` (USER), `RegisterMerchantPage` (shows the new merchant code), `NotFoundPage`, `ForbiddenPage`.
9. **Route registry + placeholders**: `src/features/user-money/routes.tsx` (yours), and **placeholder** `routes.tsx` + one "Not built yet" page in `src/features/user-history/`, `src/features/merchant/`, `src/features/admin/` with the paths and nav labels from B.6. `App.tsx` imports the four registries. After this the other two never need to touch `App.tsx`.
10. **Test helpers**: `src/test/setup.ts` (jest-dom), `src/test/renderWithProviders.tsx` (router + auth with a given session), `src/test/fakeClient.ts` (an `ApiClient` whose responses a test can script).
11. **Backend G-1** (needs the team's approval of the plan PR): add `String displayName` to `module/auth/dto/LoginResponse.java` (keep `toString()` without the token), fill it in `AuthService` login from the account, extend `AuthApiTest` to assert it. Then:
    ```powershell
    cd ..   # repository root
    mvn -B verify
    mvn test -Dtest=OpenApiDocumentationTest -Djacoco.skip=true
    Copy-Item target/openapi/openapi.json docs/api/openapi.json
    ```
    Update the login example in `docs/api/financial-ledger.postman_collection.json` if it lists response fields.
12. **Repo files**: `.gitignore` add `frontend/node_modules/`, `frontend/dist/`, `frontend/coverage/`. `AGENTS.md` command table: add `npm --prefix frontend ci`, `npm --prefix frontend test -- --run`, `npm --prefix frontend run dev`, `npm --prefix frontend run lint`. `README.md`: new section "Frontend" (Node 20+, `npm ci`, `npm run dev` with the backend on 8080, `npm run build`).
13. **ADR + brain**: `docs/adr/ADR-011-react-client-architecture.md` (stack, Vite proxy instead of CORS, `sessionStorage` + its XSS limitation, money handling, idempotency hook; must cite `[[react-client-auth-money-and-idempotency]]` - CI checks this) and the distilled note `brain/resources/react-client-auth-money-and-idempotency.md` with frontmatter `title`, `type: resource`, `authors: [Akshaya A]`, `created: <date>`. Add one line each to `brain/index.md` (Resources) and `brain/log.md` (your own lines only). Add ADR-011 to `docs/adr/README.md`.

### Tests you write (A1)

| Test | File | Checks |
|---|---|---|
| money | `src/lib/__tests__/money.test.ts` | UI-01-a, UI-02-a, UI-02-b in `ACCEPTANCE_CRITERIA.md`; same outputs as backend `MoneyFormatterTest` |
| no-float guard | `src/lib/__tests__/noFloatMoney.test.ts` | scans `src/**/*.{ts,tsx}` (Node `fs`), fails on `parseFloat(`, `toFixed(`, `/ 100` (UI-02-c) |
| error messages | `src/lib/__tests__/errorMessages.test.ts` | every code of `ErrorCode.java` has a message (UI-03-c) |
| financial hook | `src/hooks/__tests__/useFinancialSubmit.test.tsx` | new key per submit; Retry reuses key+body after network/timeout/503; `reset` -> new key; second submit while in flight sends nothing (UI-04-a..c, UI-05-a) |
| client | `src/api/__tests__/client.test.ts` | parses `ErrorResponse`; 401 calls `onUnauthorized`; timeout -> `kind: 'timeout'`; proxy 502 without JSON -> `kind: 'network'`; `Idempotent-Replayed` -> `replayed: true` |
| auth + routing | `src/auth/__tests__/*.test.tsx` | expired session = logged out; `RequireRole` -> Forbidden for wrong role (UI-03-a/b) |
| pages | `src/pages/__tests__/*.test.tsx` | login stores session and goes to the role home; register success and DUPLICATE_RESOURCE message (AUTH-1, AUTH-4); top bar shows display name (AUTH-2) |
| backend | `AuthApiTest` | login response contains `displayName` |

### Run before every push

```powershell
cd frontend
npm run lint
npm run typecheck
npm test -- --run
npm run build
cd ..
mvn -B verify
```

Manual check: start the backend (README "Run"), `npm run dev`, log in as a USER, a MERCHANT and the admin. Each sees their placeholder screen and their name in the top bar.

### Commits (small, one topic each; never `git add .`)

```powershell
git add frontend/package.json frontend/package-lock.json frontend/vite.config.ts frontend/tsconfig*.json frontend/eslint.config.js frontend/index.html frontend/src/main.tsx .gitignore
git commit -m "build(frontend): scaffold React + TypeScript + Vite client with /api proxy (Refs #<A1 issue>)"
git add frontend/src/api frontend/src/auth
git commit -m "feat(frontend): API client, error model and session handling (Refs #<A1 issue>)"
git add frontend/src/lib frontend/src/hooks
git commit -m "feat(frontend): exact money parsing/formatting and idempotent submit hook (UI-01, UI-02, UI-04, UI-05) (Refs #<A1 issue>)"
git add frontend/src/components frontend/src/styles frontend/src/App.tsx frontend/src/features frontend/src/test
git commit -m "feat(frontend): layout, role-based routing and shared components (UI-03) (Refs #<A1 issue>)"
git add frontend/src/pages
git commit -m "feat(frontend): login and registration pages (FR-AUTH-01/02/06) (Refs #<A1 issue>)"
git add src/main/java/com/quasar/ledger/module/auth src/test/java/com/quasar/ledger/module/auth docs/api
git commit -m "feat(auth): return displayName in the login response for the top bar (Refs #<A1 issue>)"
git add docs/adr/ADR-011-react-client-architecture.md docs/adr/README.md brain/resources/react-client-auth-money-and-idempotency.md brain/index.md brain/log.md README.md AGENTS.md
git commit -m "docs: ADR-011 client architecture, brain note, README and AGENTS.md frontend commands (Refs #<A1 issue>)"
git status     # must be clean; nothing of anyone else's
```

### Push and open the PR

```powershell
git push -u origin stage4/member-1-client-foundation
gh pr create --base main --title "Stage 4 (M1): React client foundation, auth pages, displayName in login" --body-file <your-pr-description.md> --reviewer SuhasiniMadala
```

PR description sections: Purpose; Requirements (list above); Implemented; Files changed; Tests run + results (paste the counts); Docs updated; Known limitations; `Closes #<A1 issue>`.

---

## PR A2 - USER Dashboard, Transfer, Pay Merchant (branch `stage4/member-1-user-money`)

Start **after A1 is merged**:

```powershell
git switch main
git pull origin main
git switch -c stage4/member-1-user-money
```

**Requirements:** FR-WAL-01..06, FR-TRF-01, 09..14, FR-PAY-01..03 (client side), UI-01, UI-02, UI-04, UI-05.
**Endpoints:** `GET/POST /api/v1/wallets`, `GET /api/v1/wallets/{id}`, `POST /api/v1/transfers` ★, `POST /api/v1/payments` ★.
**Files (all in `frontend/src/features/user-money/`):** `routes.tsx` (replace placeholder), `api.ts` (`listWallets`, `getWallet`, `createWallet`, `sendTransfer(c, body, key)`, `payMerchant(c, body, key)`), `types.ts` (`Wallet`, `TransferRequest`, `PaymentRequest`, `TransactionReceipt`), `DashboardPage.tsx`, `TransferPage.tsx`, `PayMerchantPage.tsx`, `ReceiptPanel.tsx`, `*.module.css`, `__tests__/`. Plus `frontend/tests/integration/user-money.int.test.ts`.

| Screen | Behaviour |
|---|---|
| Dashboard `/user/dashboard` | wallets table: ID + `CopyButton`, currency, `<Money>` balance, status badge; "Create wallet" with currency select (INR, USD), hides currencies the user already has; DUPLICATE_RESOURCE message if the backend refuses; empty state "No wallets yet" |
| Transfer `/user/transfer` | source = own ACTIVE wallets; destination input checked against `^W-[0-9A-Z]{10}$`; amount through `parseRupeesToPaise` (max 10,000,000 paise); description <= 140; `useFinancialSubmit`; `SubmitButton busy={inFlight}`; on 201 show `ReceiptPanel` (transaction ID, amount, from/to, time, "replayed" note if `replayed`) and re-fetch wallets; on error `ErrorBanner` with Retry when `canRetry`; editing any field calls `reset()` |
| Pay Merchant `/user/pay` | same pattern; merchant code `^MER-[0-9A-Z]{6}$`; reference <= 64 |

**Tests:** component tests per screen (loading, empty, success receipt, every 422 message, button disabled in flight, retry uses the same key); integration `user-money.int.test.ts` using Suhasini's helpers (`tests/integration/helpers.ts`, available after S1): create wallets, admin funding, transfer -> balances via `GET /wallets`, same key twice -> one transaction + `replayed` (UI-04-d), insufficient funds -> FAILED ID, pay merchant, finally `expectReconciliationPass` (USR-1..USR-6).

Commits, e.g.:

```powershell
git add frontend/src/features/user-money/api.ts frontend/src/features/user-money/types.ts frontend/src/features/user-money/DashboardPage.tsx frontend/src/features/user-money/routes.tsx
git commit -m "feat(user): wallet dashboard with create wallet and copyable wallet IDs (FR-WAL-01..06) (Refs #<A2 issue>)"
git add frontend/src/features/user-money/TransferPage.tsx frontend/src/features/user-money/ReceiptPanel.tsx
git commit -m "feat(user): transfer form with exact amounts and idempotent retry (FR-TRF-01, UI-02, UI-04, UI-05) (Refs #<A2 issue>)"
git add frontend/src/features/user-money/PayMerchantPage.tsx
git commit -m "feat(user): pay-merchant form (FR-PAY-01..03) (Refs #<A2 issue>)"
git add frontend/src/features/user-money/__tests__ frontend/tests/integration/user-money.int.test.ts
git commit -m "test(user): component and real-backend integration tests for money screens (Refs #<A2 issue>)"
git push -u origin stage4/member-1-user-money
gh pr create --base main --title "Stage 4 (M1): USER dashboard, transfer and pay merchant" --body-file <description.md> --reviewer Geethika2611
```

Run integration tests locally: start the backend with `LEDGER_ADMIN_EMAIL` / `LEDGER_ADMIN_PASSWORD` set (README "Run"), then in another terminal with the **same two variables set**: `cd frontend; npm run test:integration`.

Last commit of A2: your AI-session note `brain/projects/ai-sessions/<date>-akshaya-stage4-client.md` (goal, what worked/failed, what you reviewed and can explain) + your `brain/log.md` line.

---

## Files you own / must not touch

- **Own:** `frontend/` root config files, `frontend/src/{main.tsx,App.tsx,api,auth,lib,hooks,components,pages,styles,test}/**`, `frontend/src/features/user-money/**`, `frontend/tests/integration/user-money.int.test.ts`, backend `module/auth/**` + `AuthApiTest`, `docs/api/*` (refresh), `.gitignore`, `AGENTS.md` (frontend rows), README "Frontend" section, ADR-011 and your brain notes.
- **Do not touch** (ask the owner via an issue labelled `shared-change`): `frontend/src/features/{user-history,merchant,admin}/**` after A1 (owners: Suhasini, Suhasini, Geethika), `.github/workflows/ci.yml`, `frontend/vitest.integration.config.ts`, `frontend/tests/integration/{setup,helpers}.ts` (Suhasini), other docs.
- Others will ask **you** for shared changes (a component, a dependency, an error message). Do them in a small PR quickly.

## Verify your attribution

```powershell
git log --author="Akshaya" --oneline origin/main
gh pr view <number> --json author,commits --jq '.author.login, [.commits[].authors[].login] | unique'
```

Both must show only `Akula-Akshaya`. On GitHub the PR "Commits" tab must show your avatar on every commit.

## Done when

- [ ] A1 merged (reviewed by Suhasini or Geethika, CI green) - placeholder screens work for all roles
- [ ] A2 merged (reviewed, CI incl. frontend jobs green)
- [ ] Your rows in `ACCEPTANCE_CRITERIA.md` (UI-01..UI-05, AUTH-*, USR-1..6, SEC-2) have evidence
- [ ] ADR-011, brain note and AI-session note merged
- [ ] Demo parts D01-D06, D10, D11 and D13 (`local-check`) rehearsed
