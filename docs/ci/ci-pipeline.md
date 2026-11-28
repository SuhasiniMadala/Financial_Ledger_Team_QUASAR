# CI Plan and Results

Workflow: `.github/workflows/ci.yml`. Trigger: **every pull request** (any branch) and every push to `main`. Permissions: read-only. No repository secrets are needed (gitleaks uses the built-in `GITHUB_TOKEN`). Area note: [[ci-pipeline]].

## Jobs (SDLC section 4)

| SDLC requirement | Job | Command | Fails when |
|---|---|---|---|
| Build | `Build` | `mvn -B -ntp -DskipTests -Dcheckstyle.skip=true package` | compile/package error |
| Lint | `Lint (Checkstyle)` | `mvn -B -ntp checkstyle:check` | any rule in `config/checkstyle/checkstyle.xml` is broken |
| Automated test suite | `Tests and coverage` | `mvn -B -ntp verify` | any test fails, or coverage of ledger/transfer/idempotency/reconciliation < 70% |
| Secret scanning | `Secret scanning (gitleaks)` | `gitleaks/gitleaks-action@v2` over full history | a credential-like value is found |
| SKILL.md validation | `Validate SKILL.md files` | `mvn ... -Dtest=SkillDefinitionsTest test` | frontmatter/naming invalid, unknown field, missing skill, no distilled-note citation |
| Brain validation | `Validate brain notes` | `mvn ... -Dtest=BrainVaultTest test` | PARA folder/index/log missing, note schema invalid, broken wikilink, ADR without a distilled-note citation |
| (extra) local-check | `local-check skill on a clean machine` | `pwsh ./brain/skills/local-check/scripts/local-check.ps1` | any of the five local-check steps fails |
| Build, lint, tests (client, Stage 4) | `Frontend (lint, types, tests, build)` | `npm ci`, `npm run lint`, `npm run typecheck`, `npm test`, `npm run build` in `frontend/` | lint error (incl. `parseFloat` ban), type error, failing unit/component test, build error |
| Client against the real backend (Stage 4) | `Frontend integration (real backend)` | build jar, start it on a fresh database with a job-only admin password, `npm run test:integration` | the client's API modules and the backend disagree, a balance is wrong, or reconciliation is not PASS |

Each job has a timeout, uses the Maven cache, and prints the failing file and line. Test and coverage reports are kept as the `test-reports` artifact for 14 days; the local-check report is shown on the run's summary page.

## What changed from the Stage 1 skeleton (and why)

| Stage 1 | Problem | Stage 3 |
|---|---|---|
| "lint" = `mvn validate` | only checked the POM | Checkstyle on all main and test sources |
| brain check printed broken links as warnings | could never fail (18 broken links passed) | `BrainVaultTest` fails on any broken link or schema error |
| skill check = 3 regular expressions | did not check folder naming, field limits or citations | `SkillDefinitionsTest` implements the Agent Skills format + SB-3 |
| pushes to `main`, `akshaya/my-contribution`, `feature/**` only | other branches never ran CI | every pull request runs CI |
| validators in inline Python | not runnable on the team's Windows laptops | the same JUnit tests run locally with Maven |

## Evidence that the checks really fail

| Check | How it was made to fail | Observed (local run, 2026-11-27) |
|---|---|---|
| Brain validation | Run on the Stage 2 vault before refinement | FAIL: 24 notes "missing YAML frontmatter", 18 broken wikilinks |
| SKILL.md validation | Run on the Stage 2 skills | FAIL: skills without a distilled-note citation; `simulate-transfer` missing |
| Lint | First build of Stage 3 | FAIL: star import in `ArchitectureInvariantsTest`; later a 141-character line - both fixed |
| Tests | DEF-001, DEF-002, DEF-003 | each produced a failing build until fixed |
| Secret scanning | not demonstrated with a planted secret (the gitleaks binary could not be run in the working session) | gitleaks ran on every PR and passed; see the note below |

**Planted-failure demo (not done).** A throw-away PR with a broken wikilink, a SKILL.md without `description:`, a 141-character Java line and a fake AWS-style key was considered as extra evidence. SDLC section 4 only requires that these checks *run* on every PR, which the results below show, so the team did not do the demo in Stage 3. Checkstyle, brain and skill validation did fail for real during development (table above); for secret scanning the evidence is that the gitleaks job runs on every PR.

## Results on GitHub

| PR | Run link | Build | Lint | Tests | Secrets | Skills | Brain | local-check |
|---|---|---|---|---|---|---|---|---|
| M1 PR (#23) | [run 46012744050](https://github.com/SuhasiniMadala/Financial_Ledger_Team_QUASAR/actions/runs/46012744050) | pass | pass | pass | pass | pass | pass | pass |
| M2 PR (#24) | [run 46027083290](https://github.com/SuhasiniMadala/Financial_Ledger_Team_QUASAR/actions/runs/46027083290) | pass | pass | pass | pass | pass | pass | pass |
| M3 PR (#25) | [run 46034497855](https://github.com/SuhasiniMadala/Financial_Ledger_Team_QUASAR/actions/runs/46034497855) | pass | pass | pass | pass | pass | pass | pass |
| M3 follow-up PR (#26) | [run 46038456556](https://github.com/SuhasiniMadala/Financial_Ledger_Team_QUASAR/actions/runs/46038456556) | pass | pass | pass | pass | pass | pass | pass |

These rows are filled in by the PR author after the run finishes. Do not fill them in before the run exists.
