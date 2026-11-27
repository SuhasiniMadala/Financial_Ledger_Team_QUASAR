# Stage 4 Instructions - Geethika M (BT2024139, GitHub `Geethika2611`)

You build the **ADMIN console**: five screens that let the administrator see everything, change wallet status, add test money, inspect the ledger, reverse transactions, run reconciliation and read platform statistics. You built the ledger, idempotency, reversal and reconciliation backend in Stage 3, so you know exactly what these screens must prove.

You start after Akshaya's foundation PR (A1) is merged. You work only in `frontend/src/features/admin/` plus your own test, brain and doc files. Read once: `docs/stage-4/API_CONTRACTS.md` (Part A = backend, Part B = the shared client, hook and components you **use** but do not change).

---

## 0. One-time setup (your own computer and your own accounts)

```powershell
git clone https://github.com/SuhasiniMadala/Financial_Ledger_Team_QUASAR.git   # skip if you already have it
cd Financial_Ledger_Team_QUASAR
node -v        # v20 or newer (install Node LTS from nodejs.org if not)
java -version  # 17 or newer
git config user.name
git config user.email
gh auth status
gh api user --jq .login      # must print: Geethika2611
```

If the name/email are not yours, or `gh` shows another account:

```powershell
git config --global user.name "Geethika M"
git config --global user.email "<an email that is verified on YOUR GitHub account - see github.com -> Settings -> Emails>"
gh auth login          # GitHub.com -> HTTPS -> Login with a web browser -> sign in as Geethika2611
gh auth setup-git
```

**Shared computer only:** `gh auth login` (adds your account), `gh auth switch --user Geethika2611` before pushing, and in this repository only: `git config user.name "Geethika M"` and `git config user.email "<your verified email>"`. Check `gh api user --jq .login` and `git config user.email` before every commit. A changed display name alone is not enough.

---

## What you use from the foundation (do not re-implement)

- `useApiClient()` from `src/auth/AuthContext.tsx`; your API functions take the client: `export const listAccounts = (c: ApiClient, page: number) => c.get<Page<AccountSummary>>('/admin/accounts', { page });`
- `useFinancialSubmit` (test funding and reversal: new UUID key per submission, same key on Retry, button disabled in flight). **Never** generate keys yourself.
- `parseRupeesToPaise` and `<Money>` / `formatMinor` for every amount. `messageFor` / `ErrorBanner` for every error.
- `usePagedQuery`, `Pagination`, `DateRangeFilter`, `ConfirmDialog`, `SubmitButton`, `Loading`, `EmptyState`, `renderWithProviders`, `fakeClient`.

Missing something? Open an issue labelled `stage-4` + `shared-change`, assign Akshaya. Do not edit her files.

---

## PR G1 - Users & Merchants, Wallets (branch `stage4/member-2-admin-directory-wallets`)

```powershell
git switch main
git pull origin main
git switch -c stage4/member-2-admin-directory-wallets
```

**Requirements:** FR-ADM-01 (users and merchants), FR-ADM-02 (all accounts incl. System Funding), FR-ADM-04/05 (freeze, unfreeze, deactivate with a reason; deactivate is final), FR-WAL-07/08 (test funding), UI-01..UI-05 on these screens.

**Endpoints:** `GET /api/v1/admin/users`, `GET /api/v1/admin/merchants`, `GET /api/v1/admin/accounts` (all paged), `POST /api/v1/admin/wallets/{id}/freeze|unfreeze|deactivate` body `{reason}` (1-500 chars), `POST /api/v1/admin/wallets/{id}/test-funding` ★ body `{amountMinor, description?}`.

**Files you create (`frontend/src/features/admin/`):**

| File | Content |
|---|---|
| `routes.tsx` | **replace** the placeholder: role `ADMIN`; nav: Users & Merchants `/admin/directory`, Wallets `/admin/wallets` (G2 adds the other three) |
| `api.ts`, `types.ts` | `listUsers`, `listMerchants`, `listAccounts`, `changeWalletStatus(c, id, action, reason)`, `fundWallet(c, id, body, key)`; types `UserSummary`, `AccountSummary`, `Wallet`, `TransactionReceipt` |
| `DirectoryPage.tsx` | two tabs, Users and Merchants; paged tables: name, email, registered (local time), merchant code for merchants |
| `WalletsPage.tsx` | paged accounts table: account ID, owner name, type, currency, `<Money>` balance, status badge. Row actions: **Freeze** (ACTIVE), **Unfreeze** (FROZEN), **Deactivate** (ACTIVE or FROZEN, `ConfirmDialog` "This is final and cannot be undone"), **Fund** (ACTIVE `USER_WALLET` only). No actions on `SYS-FUND-*` rows. Link "Entries" to `/admin/accounts/:id/entries` (page built in G2). Re-fetch after every action. |
| `StatusChangeDialog.tsx` | reason textarea (required, max 500), shows INVALID_STATE_TRANSITION etc. through `messageFor` |
| `TestFundingForm.tsx` | amount via `parseRupeesToPaise` (max 10,000,000 paise), description <= 140, `useFinancialSubmit` + `SubmitButton busy={inFlight}`, receipt (transaction ID, amount, time), Retry button when `canRetry`; editing a field calls `reset()` |
| `admin.module.css`, `__tests__/` | styles and tests |

**Tests:**
- Component (`__tests__/`): directory tabs and paging; wallets table renders `<Money>`; correct actions per status and type; deactivate asks for confirmation; reason required; funding rejects `10.505`; funding button disabled while in flight and one request on double click; Retry after a 503 reuses the same key (ADM-1..ADM-4, UI-05-a).
- Integration `frontend/tests/integration/admin.int.test.ts` (uses Suhasini's helpers from S1): fund a new user's wallet and check the stored balance; freeze -> unfreeze -> deactivate; freeze a DEACTIVATED wallet -> 409 INVALID_STATE_TRANSITION; end with `expectReconciliationPass`.

**Also in G1 - fix your Stage 3 note:** in `brain/projects/ai-sessions/2026-11-27-geethika-ledger-and-reconciliation.md` the last line still says "BUILD SUCCESS - N tests". Run `mvn -B verify` on your machine and put in your real number.

**Brain:** `brain/areas/admin-console-operations.md` (frontmatter `title`, `type: area`, `authors: [Geethika M]`, `created`): why every status change needs a reason, why deactivation is final, why funding and reversal are idempotent, how to read a reconciliation report (C1-C3); link `[[financial-reconciliation-and-auditability]]`, `[[api-idempotency-and-retry-semantics]]`, FR-ADM-04/05, FR-REC-07. Add your line to `brain/index.md` (Areas) and `brain/log.md`.

**Commits** (never `git add .`):

```powershell
git add frontend/src/features/admin/api.ts frontend/src/features/admin/types.ts frontend/src/features/admin/routes.tsx frontend/src/features/admin/DirectoryPage.tsx
git commit -m "feat(admin): users and merchants directory (FR-ADM-01) (Refs #<G1 issue>)"
git add frontend/src/features/admin/WalletsPage.tsx frontend/src/features/admin/StatusChangeDialog.tsx
git commit -m "feat(admin): accounts list with freeze, unfreeze and deactivate (FR-ADM-02/04/05) (Refs #<G1 issue>)"
git add frontend/src/features/admin/TestFundingForm.tsx frontend/src/features/admin/admin.module.css
git commit -m "feat(admin): idempotent test funding form (FR-WAL-07, UI-04, UI-05) (Refs #<G1 issue>)"
git add frontend/src/features/admin/__tests__ frontend/tests/integration/admin.int.test.ts
git commit -m "test(admin): component and real-backend tests for directory, wallets and funding (Refs #<G1 issue>)"
git add brain/areas/admin-console-operations.md brain/index.md brain/log.md brain/projects/ai-sessions/2026-11-27-geethika-ledger-and-reconciliation.md
git commit -m "docs(brain): admin console operations note; record real Stage 3 verify result (Refs #<G1 issue>)"
git push -u origin stage4/member-2-admin-directory-wallets
gh pr create --base main --title "Stage 4 (M2): admin directory, wallet status and test funding" --body-file <description.md> --reviewer SuhasiniMadala
```

---

## PR G2 - Transactions & Ledger, Reconciliation, Statistics (branch `stage4/member-2-admin-ledger-reconciliation`)

Start **after G1 is merged** (same folder):

```powershell
git switch main
git pull origin main
git switch -c stage4/member-2-admin-ledger-reconciliation
```

**Requirements:** FR-ADM-03 (transactions with entries), FR-LED-07..10 (reversal: COMPLETED only, once, reason 10-500, mirror entries), FR-REC-01..08 (reconciliation report, PASS only with no discrepancy), FR-ADM-07 (statistics), UI-04/UI-05 on the reversal form.

**Endpoints:** `GET /api/v1/admin/transactions` (`type, status, walletId, from, to, page, size`), `GET /api/v1/admin/transactions/{id}`, `POST /api/v1/admin/transactions/{id}/reversal` ★ `{reason}`, `GET /api/v1/admin/accounts/{id}/entries`, `POST /api/v1/admin/reconciliations` (no body, no key), `GET /api/v1/admin/statistics`.

| File | Content |
|---|---|
| `routes.tsx` | add nav: Transactions & Ledger `/admin/transactions`, Reconciliation `/admin/reconciliation`, Statistics `/admin/statistics`; routes `/admin/transactions/:id`, `/admin/accounts/:id/entries` |
| `TransactionsPage.tsx` | filters (type, status, wallet ID with `W-` format check, date range), paged table: time, ID, type, status (+ failure code), `<Money>` amount, source, destination, "reversed by" link |
| `TransactionDetailPage.tsx` | all `TransactionView` fields, entries table (entry, account, DEBIT/CREDIT, amount, balance after), a row "Total debits = total credits" computed with integer addition; links to the original/reversal; **Reverse** button only if `status=COMPLETED`, `type != REVERSAL` and no `reversedBy` |
| `ReversalForm.tsx` | reason 10-500 chars (counter), `ConfirmDialog`, `useFinancialSubmit`; on 201 show the REVERSAL receipt and reload the detail; ALREADY_REVERSED / NOT_REVERSIBLE / INSUFFICIENT_FUNDS / WALLET_NOT_ACTIVE via `messageFor` |
| `AccountEntriesPage.tsx` | paged entries for one account (sequence, time, transaction link, direction, amount, balance after) |
| `ReconciliationPage.tsx` | "Run reconciliation" (button disabled while running); result badge PASS/FAIL; run time and duration; per-currency debits/credits table; counts checked; discrepancy table (type, subject, expected, actual, difference) or "No discrepancies" |
| `StatisticsPage.tsx` | users, merchants, wallets by status, transactions by status, completed totals per type and currency (`<Money>`), latest reconciliation |

**Tests:** component tests for each page (loading, empty, error, success; Reverse hidden for FAILED/REVERSAL/already reversed; reason length; confirmation; disabled while in flight; Retry reuses the key; reconciliation FAIL shows the discrepancy table). Integration (`admin.int.test.ts`, add cases): a transfer between two helper users -> detail shows 2 balanced entries; reverse it -> balances restored; reverse again -> 409 ALREADY_REVERSED; reverse a FAILED one -> 422 NOT_REVERSIBLE; reconciliation PASS with currency totals; statistics counts increase (ADM-5..ADM-9).

**Commits:** one per screen (`feat(admin): transactions list with filters (FR-ADM-03)`, `feat(admin): transaction detail with ledger entries and reversal (FR-LED-07..10, UI-04, UI-05)`, `feat(admin): account entries view`, `feat(admin): reconciliation report screen (FR-REC-01..08)`, `feat(admin): platform statistics (FR-ADM-07)`, `test(admin): ...`), each with `(Refs #<G2 issue>)`. Last commit: your AI-session note `brain/projects/ai-sessions/<date>-geethika-stage4-admin.md` + your `brain/log.md` line.

```powershell
git push -u origin stage4/member-2-admin-ledger-reconciliation
gh pr create --base main --title "Stage 4 (M2): admin transactions, ledger, reversal, reconciliation and statistics" --body-file <description.md> --reviewer Akula-Akshaya
```

---

## Run before every push

```powershell
cd frontend
npm ci
npm run lint
npm run typecheck
npm test -- --run
npm run build
```

Integration tests: start the backend with `LEDGER_JWT_SECRET`, `LEDGER_ADMIN_EMAIL`, `LEDGER_ADMIN_PASSWORD` set (README "Run"); in a second terminal set the **same** admin variables, then `cd frontend; npm run test:integration`. Manual check: `npm run dev`, log in as admin, do every action once against the local backend, and finish with reconciliation PASS.

## Files you own / must not touch

- **Own:** `frontend/src/features/admin/**`, `frontend/tests/integration/admin.int.test.ts`, `brain/areas/admin-console-operations.md`, your AI-session notes, your lines in `brain/index.md` / `brain/log.md`.
- **Do not touch:** anything else in `frontend/` (owner Akshaya: `src/api`, `src/auth`, `src/lib`, `src/hooks`, `src/components`, `src/styles`, `App.tsx`, `package.json`; owner Suhasini: `features/user-history`, `features/merchant`, `tests/integration/{setup,helpers}.ts`, `vitest.integration.config.ts`), `.github/workflows/ci.yml`, backend code. Ask via an issue labelled `shared-change`.
- Do **not** run `npm install <package>` on your branch: dependencies are Akshaya's.

## Keep your branch current

After any merge to `main`: `git pull origin main` (merge, never rebase), then `npm --prefix frontend ci` if `package-lock.json` changed, then `git push` (never `--force`). A conflict in `brain/index.md` / `brain/log.md`: keep both sides.

## Verify your attribution

```powershell
git log --author="Geethika" --oneline origin/main
gh pr view <number> --json author,commits --jq '.author.login, [.commits[].authors[].login] | unique'
```

Both must show only `Geethika2611`.

## Done when

- [ ] G1 and G2 merged (independent review, CI green)
- [ ] ADM-1..ADM-9 in `ACCEPTANCE_CRITERIA.md` have evidence
- [ ] Stage 3 "N tests" fixed; area note and AI-session note merged
- [ ] Demo parts D04, D09 (admin) and D14 (`brain-query` live) rehearsed
