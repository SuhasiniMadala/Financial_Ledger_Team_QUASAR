# Stage 4 Task Dependencies and Integration Order

Branches follow `docs/branching-strategy.md`: `stage4/member-<n>-<area>`, one branch per PR, always created from an up-to-date `main`. M1 = Akshaya (`Akula-Akshaya`), M2 = Geethika (`Geethika2611`), M3 = Suhasini (`SuhasiniMadala`).

## 1. Pull requests in order

| Step | PR | Branch | Owner | Depends on | Reviewer | Can run in parallel? |
|---|---|---|---|---|---|---|
| 0 | P0 Stage 4 plan (these documents) | `stage4/member-3-plan` | Suhasini | - | Akshaya **and** Geethika (both approve the allocation) | - |
| 1 | A1 Client foundation, login/register, `displayName` in login, ADR-011 | `stage4/member-1-client-foundation` | Akshaya | P0 merged | Suhasini | **No - everyone waits for A1** |
| 2 | S1 Frontend CI jobs + integration-test harness | `stage4/member-3-frontend-ci` | Suhasini | A1 merged | Akshaya | yes, with steps 3-5 (should merge first, it is small) |
| 3 | A2 USER Dashboard, Transfer, Pay Merchant | `stage4/member-1-user-money` | Akshaya | A1 | Geethika | yes |
| 3 | G1 ADMIN Directory + Wallets (status changes, test funding) | `stage4/member-2-admin-directory-wallets` | Geethika | A1 | Suhasini | yes |
| 3 | S2 USER History & Detail + MERCHANT Account, Payments, Statistics | `stage4/member-3-history-merchant` | Suhasini | A1 | Geethika | yes |
| 4 | G2 ADMIN Transactions & Ledger (reversal), Reconciliation, Statistics | `stage4/member-2-admin-ledger-reconciliation` | Geethika | G1 merged (same `features/admin/` files) | Akshaya | yes, with A2 and S2 |
| 5 | Full UI test-matrix run + demo rehearsal | (no PR - results go into S3) | all three | A2, G2, S2 merged | - | - |
| 6 | S3 Docs, traceability, test report, TQ-7 skill runs, review note, CHANGELOG, version | `stage4/member-3-docs-release` | Suhasini | step 5 done | Akshaya and Geethika | No |
| 7 | Tag the final release (after **explicit team approval**) | - | Suhasini | S3 merged, CI green on `main` | - | No |

Planning order inside a step: S1 should merge before A2, G1 and S2 merge, so those PRs are already checked by the frontend CI jobs. If S1 is late, the reviewer of each PR runs `npm run lint`, `npm run typecheck`, `npm test` and `npm run build` locally and writes the result in the review.

## 2. Contracts that must exist before parallel work (all in A1)

`docs/stage-4/API_CONTRACTS.md` Part B: folder layout, `ApiClient` + `ApiError`, `useApiClient`, `AuthContext` + `RequireRole`, the role/route registry with placeholder `routes.tsx` for `user-history`, `merchant` and `admin`, `money.ts`, `errorMessages.ts` (all 17 codes), `useFinancialSubmit`, `usePagedQuery`, shared components, `global.css`, `renderWithProviders` + `fakeClient` test helpers.

A1 is "done" only when a placeholder screen for every role renders behind the correct guard. Then the other members can start without touching A1's files.

## 3. Shared files and their single owner

| File(s) | Owner | Others who need a change |
|---|---|---|
| `frontend/package.json`, `package-lock.json`, `vite.config.ts`, `tsconfig.json`, `eslint.config.js`, `index.html` | Akshaya | open an issue (`stage-4`, `shared-change`); Akshaya adds the dependency in a small PR. Do not run `npm install <pkg>` on your branch. |
| `frontend/src/App.tsx`, `main.tsx`, `src/api/**`, `src/auth/**`, `src/lib/**`, `src/hooks/**`, `src/components/**`, `src/styles/global.css`, `src/pages/**`, `src/test/**` | Akshaya | same issue process |
| `frontend/src/features/<x>/routes.tsx` | owner of feature `<x>` | - |
| `.github/workflows/ci.yml`, `frontend/vitest.integration.config.ts`, `frontend/tests/integration/setup.ts`, `helpers.ts` | Suhasini | issue to Suhasini |
| `.gitignore`, `AGENTS.md` (frontend commands), `README.md` section "Frontend" | Akshaya (in A1) | - |
| `README.md` other sections, `docs/user-guide.md`, `docs/architecture/traceability-matrix.md`, `docs/testing/*`, `CHANGELOG.md`, `pom.xml` `<version>` | Suhasini (S3) | send her the facts in your PR description |
| Backend `module/auth/**` | Akshaya | - |
| `brain/index.md`, `brain/log.md` | everyone appends **only their own lines** | see section 5 |

## 4. When to update your branch

After **every** merge into `main`, everyone with an open branch runs:

```powershell
git switch <your-branch>
git pull origin main          # a merge, never a rebase: keeps everyone's commits as they are
npm --prefix frontend ci      # only if package-lock.json changed
git push                      # no --force, ever
```

## 5. Conflicts

- Because of the route registry and per-feature folders, code conflicts should only happen in `brain/index.md` and `brain/log.md`. In those files keep **both** sides (everyone's lines), remove the conflict markers, then `git add` the file and `git commit`.
- `package-lock.json` conflict: never hand-edit it. Only Akshaya changes dependencies, so take `main`'s version: during `git pull origin main`, `--theirs` is `main`, so run `git checkout --theirs frontend/package-lock.json`, then `npm --prefix frontend ci`, `git add frontend/package-lock.json` and `git commit`.
- Anything else: stop and ask the file's owner. Never resolve a conflict by deleting someone else's code.

## 6. Critical path

P0 -> A1 -> (S1, A2, G1, S2 in parallel) -> G2 -> full test run + rehearsal -> S3 -> tag. A1 is the bottleneck, so Akshaya does nothing else until A1 is merged, and the other two review it within a day.
