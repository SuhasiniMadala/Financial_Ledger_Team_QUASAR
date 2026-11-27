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
| Secret scanning | not demonstrated locally (the gitleaks binary could not be run in the working session) | to be demonstrated on GitHub, see below |

**To do on GitHub (owner: Suhasini, after the first PR is green):** open a throw-away pull request from a branch `ci/demo-failure` that (1) adds a broken wikilink to a brain note, (2) removes `description:` from one SKILL.md, (3) adds a line longer than 140 characters to a Java file and (4) adds a fake AWS-style key such as `AKIAIOSFODNN7EXAMPLE` to a text file. Record the run link and which jobs failed below, then **close the PR without merging** and delete the branch.

## Results on GitHub

| PR | Run link | Build | Lint | Tests | Secrets | Skills | Brain | local-check |
|---|---|---|---|---|---|---|---|---|
| M1 PR | (add link) | | | | | | | |
| M2 PR | (add link) | | | | | | | |
| M3 PR | (add link) | | | | | | | |
| CI failure demo | (add link) | | | | | | | |

These rows are filled in by the PR author after the run finishes. Do not fill them in before the run exists.
