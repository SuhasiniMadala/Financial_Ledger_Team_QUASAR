# Branching Strategy and Pull Request Rules

Required by SDLC section 4 ("documented branching strategy").

## Branches

| Branch | Purpose | Who writes to it |
|---|---|---|
| `main` | Always working, reviewed code. Release tags are made here. | Nobody directly - only merged pull requests |
| `stage3/member-1-account` | Akshaya's Stage 3 work (accounts, wallets, platform) | Akshaya |
| `stage3/member-2-payments-ledger` | Geethika's Stage 3 work (ledger, idempotency, reconciliation, brain) | Geethika |
| `stage3/member-3-transfers-history` | Suhasini's Stage 3 work (transfers, payments, history, CI) | Suhasini |

Rules:
1. Create your branch from an up-to-date `main` (`git switch main`, `git pull`, `git switch -c <branch>`).
2. Commit only your own work, from your own computer and Git identity, so contribution is visible in history (SB-6).
3. Commit at meaningful checkpoints (a feature with its tests, a defect with its regression test, a document section) - not one giant commit.
4. Never push to `main`, never force-push, never delete someone else's branch.

## Pull requests

- One PR per meaningful piece of work; the description says: purpose, `Closes #<issue>`, requirements, files, tests run and results, defects fixed, docs updated, risks.
- **At least one other member reviews and approves.** Nobody approves their own PR.
- **CI must be green** (build, lint, tests, secret scan, skills, brain, local-check).
- The reviewer checks they can explain the change (SDLC 6) and that AI-generated code was reviewed by the author.
- Merge with "Create a merge commit" so each member's commits stay visible.

## Stage 3 merge order

The modules build on each other, so PRs are merged in this order: **M1 -> M2 -> M3**. After a PR is merged, the next member updates their branch from `main` before opening theirs.

## Releases

- Tags follow `v<major>.<minor>.<patch>-stage<N>`; Stage 3 is `v0.3.0-stage3`.
- Tag only after every Stage 3 PR is merged and CI on `main` is green. Record the release in `CHANGELOG.md`.

## Branch protection (repository owner, once)

GitHub -> repository -> Settings -> Branches -> Add branch protection rule -> Branch name pattern `main` -> tick "Require a pull request before merging" (1 approval) and "Require status checks to pass" (select the CI jobs) -> Save.
