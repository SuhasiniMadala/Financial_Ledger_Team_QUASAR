# Stage 4 Instructions - Suhasini M (BT2024043, GitHub `SuhasiniMadala`)

You own four things: (1) this **plan** and the team's GitHub set-up; (2) **frontend CI** and the **integration-test harness** that runs the client code against the real backend; (3) the **USER History & Detail** and all three **MERCHANT** screens; (4) the **final docs, demo coordination and release**.

Read once: `docs/stage-4/API_CONTRACTS.md` (Part A = backend, Part B = shared client, hook and components you **use** but do not change).

---

## 0. One-time setup (your own computer and your own accounts)

```powershell
cd C:\Users\New\Documents\Financial_Ledger_Team_QUASAR
node -v; java -version
git config user.name          # Suhasini M
git config user.email         # the email GitHub links to SuhasiniMadala (Stage 3 commits used it)
gh auth status                # token scopes must include 'workflow' (you edit ci.yml)
gh api user --jq .login       # must print: SuhasiniMadala
```

If `workflow` is missing: `gh auth refresh -h github.com -s workflow`. **Never** run Git commands for Akshaya or Geethika from your account, and never commit their files. If they work on your laptop, they must `gh auth switch --user <their login>` and set the repo-local `git config user.name/user.email` to their own values first, and you switch back afterwards (`gh auth switch --user SuhasiniMadala`).

---

## P0 - Plan PR (branch `stage4/member-3-plan`) - this PR

1. Push the branch and open the PR with **both** teammates as reviewers. Ask them to comment on the allocation, G-1 (`displayName` in login) and the version name. Change the plan in this PR if they disagree.
2. **Source documents:** get SRS v1.4 and the Project Statement from whoever has them. Add them to the repository in this PR (replace `Financial_Wallet_Ledger_SRS (2) (1).docx`), and re-check SRS 3.1, UI-01..UI-05 and Table 3-1 against `MASTER_PLAN.md`. If anything differs, fix the plan before merging.
3. After both approve and you merge (merge commit):
   - **Issues** (SDLC 4 "GitHub Issues for all work"), one per PR, assigned to the owner:
     ```powershell
     gh issue create --title "[M1] Stage 4 A1: React client foundation, auth pages, displayName" --assignee Akula-Akshaya --label stage-4 --body "See docs/stage-4/INSTRUCTIONS_AKSHAYA.md (PR A1)."
     gh issue create --title "[M1] Stage 4 A2: USER dashboard, transfer, pay merchant" --assignee Akula-Akshaya --label stage-4 --body "See docs/stage-4/INSTRUCTIONS_AKSHAYA.md (PR A2)."
     gh issue create --title "[M2] Stage 4 G1: admin directory, wallet status, test funding" --assignee Geethika2611 --label stage-4 --body "See docs/stage-4/INSTRUCTIONS_GEETHIKA.md (PR G1)."
     gh issue create --title "[M2] Stage 4 G2: admin transactions, reversal, reconciliation, statistics" --assignee Geethika2611 --label stage-4 --body "See docs/stage-4/INSTRUCTIONS_GEETHIKA.md (PR G2)."
     gh issue create --title "[M3] Stage 4 S1: frontend CI and integration-test harness" --assignee SuhasiniMadala --label stage-4 --body "See docs/stage-4/INSTRUCTIONS_SUHASINI.md (PR S1)."
     gh issue create --title "[M3] Stage 4 S2: USER history/detail and MERCHANT screens" --assignee SuhasiniMadala --label stage-4 --body "See docs/stage-4/INSTRUCTIONS_SUHASINI.md (PR S2)."
     gh issue create --title "[M3] Stage 4 S3: docs, traceability, test report, review note, release" --assignee SuhasiniMadala --label stage-4 --body "See docs/stage-4/INSTRUCTIONS_SUHASINI.md (PR S3)."
     ```
     (Create the `stage-4` label first if it does not exist: `gh label create stage-4`.) Post the issue numbers in the team chat.
   - **Branch protection** (you are the repo owner): GitHub -> Settings -> Branches -> Add rule -> `main` -> "Require a pull request before merging" (1 approval) and "Require status checks to pass" (select the CI jobs) -> Save.

---

## PR S1 - Frontend CI + integration harness (branch `stage4/member-3-frontend-ci`)

Start after **A1** is merged:

```powershell
git switch main; git pull origin main
git switch -c stage4/member-3-frontend-ci
```

**Why:** SDLC 4 requires build, lint and the automated tests on every PR. Integration tests must prove the client works against the **real** backend, not mocks (AGENTS.md testing rules).

1. **`.github/workflows/ci.yml`**: keep the 7 existing jobs unchanged and add two:
   ```yaml
   frontend:
     name: Frontend (lint, types, unit tests, build)
     runs-on: ubuntu-latest
     timeout-minutes: 15
     defaults:
       run:
         working-directory: frontend
     steps:
       - uses: actions/checkout@v4
       - uses: actions/setup-node@v4
         with:
           node-version: '20'
           cache: npm
           cache-dependency-path: frontend/package-lock.json
       - run: npm ci
       - run: npm run lint
       - run: npm run typecheck
       - run: npm test -- --run
       - run: npm run build

   frontend-integration:
     name: Frontend integration (real backend)
     runs-on: ubuntu-latest
     timeout-minutes: 20
     steps:
       - uses: actions/checkout@v4
       - uses: actions/setup-java@v4
         with:
           java-version: '17'
           distribution: temurin
           cache: maven
       - uses: actions/setup-node@v4
         with:
           node-version: '20'
           cache: npm
           cache-dependency-path: frontend/package-lock.json
       - name: Build the backend jar
         run: mvn -B -ntp -DskipTests -Dcheckstyle.skip=true package
       - name: Start the backend on a fresh database (secrets generated for this run only)
         run: |
           ADMIN_PASSWORD="$(openssl rand -hex 16)"
           echo "::add-mask::$ADMIN_PASSWORD"
           echo "LEDGER_ADMIN_EMAIL=ci-admin@ci.test" >> "$GITHUB_ENV"
           echo "LEDGER_ADMIN_PASSWORD=$ADMIN_PASSWORD" >> "$GITHUB_ENV"
           LEDGER_JWT_SECRET="$(openssl rand -hex 32)" LEDGER_ADMIN_EMAIL=ci-admin@ci.test LEDGER_ADMIN_PASSWORD="$ADMIN_PASSWORD" \
             nohup java -jar target/financial-ledger-*.jar --ledger.db.path="$RUNNER_TEMP/it.db" > "$RUNNER_TEMP/server.log" 2>&1 &
           for i in $(seq 1 60); do curl -fs http://localhost:8080/api/v1/health && exit 0; sleep 2; done
           echo "Backend did not start"; cat "$RUNNER_TEMP/server.log"; exit 1
       - run: npm ci
         working-directory: frontend
       - run: npm run test:integration
         working-directory: frontend
         env:
           LEDGER_BASE_URL: http://localhost:8080
       - name: Server log (on failure)
         if: failure()
         run: tail -n 200 "$RUNNER_TEMP/server.log"
   ```
   No repository secrets are used. The admin password exists only inside the run and is masked in the log.
2. **`frontend/vitest.integration.config.ts`**: `environment: 'node'`, `include: ['tests/integration/**/*.int.test.ts']`, `setupFiles: ['tests/integration/setup.ts']`, `testTimeout: 30000`, `fileParallelism: false`.
3. **`frontend/tests/integration/setup.ts`**: fail fast with a clear message if `LEDGER_ADMIN_EMAIL` / `LEDGER_ADMIN_PASSWORD` are not set or `GET /api/v1/health` is not UP.
4. **`frontend/tests/integration/helpers.ts`** (the contract everyone's integration tests use; uses Akshaya's `createApiClient` with `baseUrl = process.env.LEDGER_BASE_URL ?? 'http://localhost:8080'`):
   ```ts
   export function uniqueEmail(prefix: string): string;                  // e.g. asha-<time>-<random>@it.test
   export async function adminClient(): Promise<ApiClient>;
   export async function newUser(name?: string): Promise<{ client: ApiClient; accountId: string }>;
   export async function newMerchant(name?: string): Promise<{ client: ApiClient; accountId: string; merchantCode: string; walletId: string }>;
   export async function newFundedWallet(user: ApiClient, admin: ApiClient, paise: number, currency?: 'INR' | 'USD'): Promise<string>;
   export async function storedBalance(owner: ApiClient, walletId: string): Promise<number>;   // GET /wallets/{id}
   export async function expectReconciliationPass(admin: ApiClient): Promise<void>;
   ```
5. **`frontend/tests/integration/smoke.int.test.ts`**: health UP; register + login a user (login has `displayName`); admin login; reconciliation PASS.
6. **Brain:** `brain/resources/client-integration-testing-against-real-backend.md` (frontmatter `type: resource`, `authors: [Suhasini M]`): why mocks are not evidence for money, fresh database per run, secrets generated in the job, checking stored balances, ending with reconciliation; cite `[[testing-concurrency-and-failure-injection]]`, `[[testing-strategy-and-quality-gates]]`. Add your lines to `brain/index.md` and `brain/log.md`. Update `docs/ci/ci-pipeline.md` (jobs table) and `brain/areas/ci-pipeline.md`.

```powershell
cd frontend; npm ci; npm run lint; npm run typecheck; npm test -- --run; npm run build; cd ..
# with a backend running locally (README "Run") and the same admin variables set in this terminal:
cd frontend; npm run test:integration; cd ..
git add .github/workflows/ci.yml docs/ci/ci-pipeline.md brain/areas/ci-pipeline.md
git commit -m "ci: frontend lint/types/unit/build job and real-backend integration job (Refs #<S1 issue>)"
git add frontend/vitest.integration.config.ts frontend/tests/integration/setup.ts frontend/tests/integration/helpers.ts frontend/tests/integration/smoke.int.test.ts
git commit -m "test(frontend): integration harness against a real backend on a fresh database (Refs #<S1 issue>)"
git add brain/resources/client-integration-testing-against-real-backend.md brain/index.md brain/log.md
git commit -m "docs(brain): distilled note on client integration testing (Refs #<S1 issue>)"
git push -u origin stage4/member-3-frontend-ci
gh pr create --base main --title "Stage 4 (M3): frontend CI and integration-test harness" --body-file <description.md> --reviewer Akula-Akshaya
```

---

## PR S2 - USER History & Detail, MERCHANT screens (branch `stage4/member-3-history-merchant`)

Start after **A1** is merged (in parallel with S1):

```powershell
git switch main; git pull origin main
git switch -c stage4/member-3-history-merchant
```

**Requirements:** FR-LED-11 (list own transactions incl. incoming and own FAILED, newest first, filters), FR-LED-12 (detail with own entries only, counterparty name, failure code, reversal link), FR-AUTH-09 (others' transaction = not found), FR-PAY-04 (merchant account), FR-PAY-05/06 (payments + reversals, payer name and reference, never payer wallet/balance, date filter), FR-PAY-07 (count, total, half-even average, default 30 days), UI-01, UI-03.

**Endpoints:** `GET /api/v1/transactions` (`walletId, type, status, from, to, page, size`), `GET /api/v1/transactions/{id}`, `GET /api/v1/merchant/account`, `GET /api/v1/merchant/transactions` (`from, to, page, size`), `GET /api/v1/merchant/statistics` (`from, to`). You may **import** (not edit) `listWallets` from `features/user-money/api.ts` for the wallet filter.

| File | Content |
|---|---|
| `features/user-history/routes.tsx` | **replace** placeholder: role USER, nav "History" (order after Akshaya's items), routes `/user/history`, `/user/history/:id` |
| `features/user-history/api.ts`, `types.ts` | `listTransactions(c, filters, page)`, `getTransaction(c, id)`; `UserTransactionView`, `UserTransactionDetail`, `LedgerEntryView` |
| `HistoryPage.tsx` | filters: wallet (own wallets), type, status, date range; paged table: time, type, direction (IN/OUT), wallet, counterparty, `<Money>` amount, status (FAILED + readable failure code), link to detail; empty state |
| `TransactionDetailPage.tsx` | all fields of FR-LED-12; own entries table; links "reversal of" / "reversed by"; 404 -> "Transaction not found" (FR-AUTH-09) |
| `features/merchant/routes.tsx` | **replace** placeholder: role MERCHANT, nav Account / Payments / Statistics |
| `features/merchant/api.ts`, `types.ts` | `getMerchantAccount`, `listMerchantTransactions`, `getMerchantStatistics` |
| `AccountPage.tsx` | business name, merchant code (+ `CopyButton`), wallet ID, `<Money>` balance, status |
| `PaymentsPage.tsx` | date range filter, paged table: time, type (PAYMENT / REVERSAL), direction, `<Money>` amount, payer name, reference; **no payer wallet ID or balance anywhere** |
| `StatisticsPage.tsx` | date range (empty = backend default 30 days), shows the returned from/to, count, `<Money>` total and average; note that a reversed payment is still counted (see `docs/modules/transfers-payments-history.md`) |

**Tests:** component tests (loading, empty, error, success; filters build the right query; FAILED rows show the failure message; detail 404; merchant pages render no `W-` IDs except the merchant's own wallet). Integration: `tests/integration/history.int.test.ts` (transfer A->B: A sees OUTGOING, B sees INCOMING, A's FAILED attempt visible only to A, B cannot open A's FAILED transaction, detail shows own entries only) and `tests/integration/merchant.int.test.ts` (payment appears with payer name and reference; response JSON contains no payer wallet ID; statistics count/total/average; date filter excludes old ranges), each ending with `expectReconciliationPass` (USR-7..9, MER-1..4).

Commits: `feat(user): transaction history with filters (FR-LED-11)`, `feat(user): transaction detail with own entries and reversal links (FR-LED-12)`, `feat(merchant): account screen (FR-PAY-04)`, `feat(merchant): payments list with date filter and payer privacy (FR-PAY-05/06)`, `feat(merchant): payment statistics (FR-PAY-07)`, `test(...)`, each `(Refs #<S2 issue>)`, adding only those files. Then:

```powershell
git push -u origin stage4/member-3-history-merchant
gh pr create --base main --title "Stage 4 (M3): USER history/detail and MERCHANT screens" --body-file <description.md> --reviewer Geethika2611
```

---

## Step 5 - Full test run and demo rehearsal (all three, you coordinate)

After A2, G2 and S2 are merged: book a session; run every row of `ACCEPTANCE_CRITERIA.md`, fill "Actual result" and "Evidence" honestly (FAIL stays FAIL; open an issue + regression test for each real defect - TQ-8, `docs/testing/defect-log.md`); rehearse `DEMONSTRATION_CHECKLIST.md` D01-D16 with each presenter.

## PR S3 - Docs, traceability, test report, review note, release prep (branch `stage4/member-3-docs-release`)

| File | Update |
|---|---|
| `docs/stage-4/ACCEPTANCE_CRITERIA.md`, `docs/stage-4/evidence/` | results + screenshots (check: no secrets visible) |
| `docs/user-guide.md` | rewrite per role for the React client (keep a short "API / Postman" appendix) |
| `README.md` | run both backend and frontend; project structure incl. `frontend/` |
| `docs/architecture/traceability-matrix.md` | UI-01..UI-05 and every FR now covered by a screen: add PRs, component/integration tests, skills, brain notes |
| `docs/testing/test-execution-report.md` | add frontend unit/component/integration results (real counts from CI), Stage 4 manual matrix summary |
| `docs/testing/skill-evaluations.md` | TQ-7: re-run `simulate-transfer -Evaluate`, `local-check`, `brain-query` and record results |
| `docs/testing/defect-log.md` | any Stage 4 defects with regression tests |
| `brain/projects/stage-4-review.md` | SB-2 review note (what worked / did not / what to change); ask both teammates to comment in the PR |
| `brain/projects/ai-sessions/<date>-suhasini-stage4.md` | your AI-session note |
| `brain/index.md`, `brain/log.md` | your lines |
| `docs/architecture/development-plan.md` | Stage 4 tasks ticked with PRs |
| `CHANGELOG.md`, `pom.xml` `<version>` | release entry (proposed `v1.0.0-stage4`, version `1.0.0`) - only the version the team approved |

```powershell
git push -u origin stage4/member-3-docs-release
gh pr create --base main --title "Stage 4 (M3): final docs, test results, review note and release notes" --body-file <description.md> --reviewer Akula-Akshaya --reviewer Geethika2611
```

## Release (only after explicit approval)

When S3 is merged, `main` CI is green and **both** teammates have written "approved for release" on the S3 PR:

```powershell
git switch main; git pull origin main
gh run list --branch main --limit 1          # must be success
git tag -a v1.0.0-stage4 -m "Stage 4: React client, live demo, final release"   # use the approved name
git push origin v1.0.0-stage4
```

## Files you own / must not touch

- **Own:** `docs/stage-4/**`, `.github/workflows/ci.yml`, `frontend/vitest.integration.config.ts`, `frontend/tests/integration/{setup,helpers,smoke,history,merchant}*`, `frontend/src/features/user-history/**`, `frontend/src/features/merchant/**`, `docs/user-guide.md`, `docs/testing/**`, traceability matrix, development plan, `CHANGELOG.md`, `pom.xml` version, README (non-frontend sections), your brain notes and lines.
- **Do not touch:** Akshaya's foundation (`frontend/src/{api,auth,lib,hooks,components,pages,styles,test}`, `App.tsx`, `package.json`, `features/user-money`, backend auth) and Geethika's `features/admin`. Ask via an issue labelled `shared-change`. Never `npm install <package>` on your branch.

## Verify your attribution

```powershell
git log --author="Suhasini" --oneline origin/main
gh pr view <number> --json author,commits --jq '.author.login, [.commits[].authors[].login] | unique'   # only SuhasiniMadala
```

## Done when

- [ ] P0 merged with both approvals; issues created; branch protection on
- [ ] S1, S2, S3 merged after independent review, CI green
- [ ] USR-7..9, MER-1..4, INT-1 rows have evidence; full matrix run coordinated
- [ ] Demo D07, D08, D12, D15, D16 rehearsed
- [ ] Release tag pushed after explicit team approval
