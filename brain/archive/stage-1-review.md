---
title: "Stage 1 review (Requirements) - written retrospectively"
type: review
authors: [Akshaya A]
created: 2026-11-27
tags: [stage-1, review, sb-2]
---

# Stage 1 review (Requirements)

SB-2 asks for a short review note at the end of each stage. This one was **not** written at the end of Stage 1; it was written on 2026-11-27, at the start of Stage 3, from the repository history and the Stage 3 audit. That gap is itself the first lesson below.

## What worked

- The SRS (v1.2) is detailed and testable: every requirement has an ID, a priority and stimulus/response scenarios. Stage 3 tests map one-to-one onto those scenarios.
- All three members ingested research notes (17 notes in total, visible in git history), and they were distilled into 7 resource notes such as [[double-entry-accounting-principles]].
- `AGENTS.md` and a CI skeleton existed from the start.

## What did not work

- The CI "lint" step only ran `mvn validate`, and brain validation printed broken links as warnings without failing - so CI was green while 18 wikilinks were broken.
- Research notes had no metadata, so authorship was only visible in git, not in the notes.
- Some notes duplicated each other (three on concurrency, two on ACID, two on WAL).
- Three of the first four pull requests were merged without a review.

## What to change

- Make every check fail loudly (done in Stage 3: Checkstyle, `BrainVaultTest`, `SkillDefinitionsTest`).
- Write the stage review note *during* the last week of the stage, not afterwards.
- Require one approving review on `main` through branch protection.
