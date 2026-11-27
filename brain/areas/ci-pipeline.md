---
title: "Standing rules: the CI pipeline"
type: area
authors: [Suhasini M]
created: 2026-11-27
tags: [ci, github-actions]
requirements: [TQ-5, TQ-7]
---

# Standing rules: the CI pipeline

GitHub Actions workflow `.github/workflows/ci.yml` runs on **every pull request** and on pushes to `main`. Details and evidence: `docs/ci/ci-pipeline.md`.

| Job | What it proves | Fails when |
|---|---|---|
| Build | the code compiles and packages | compile error |
| Lint | Checkstyle rules (`config/checkstyle/checkstyle.xml`) | any violation |
| Tests | full suite + JaCoCo coverage rule on a fresh database | any test fails, coverage < 70% |
| Secret scan | gitleaks over the whole git history | a key, token or password is committed |
| Skills | every `SKILL.md` follows the Agent Skills format and cites a distilled note | `SkillDefinitionsTest` fails |
| Brain | PARA structure, note schema, no broken wikilinks, ADR citations | `BrainVaultTest` fails |
| local-check | the `local-check` skill passes on a clean Linux machine | any of its 5 steps fails |

## Rules

- Never merge a red pull request. Fix the cause; never delete or skip a failing test to get green.
- The validators are ordinary JUnit tests, so the same check runs on a laptop: `mvn test -Dtest=BrainVaultTest -Djacoco.skip=true`.
- Workflow permissions are read-only; no repository secrets are needed.
- A deliberately failing pull request is used once per stage to prove each check really fails (evidence in `docs/ci/ci-pipeline.md`).

Related: [[testing-strategy-and-quality-gates]], [[testing-concurrency-and-failure-injection]].
