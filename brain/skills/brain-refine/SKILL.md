---
name: brain-refine
description: Refine the Financial Ledger second brain - distil inbox notes into brain/resources/, merge duplicates, move processed raw notes to brain/research/ or brain/archive/, add missing frontmatter and fix broken wikilinks - then prove the vault is valid with the brain validator. Use at the end of each stage or when the inbox grows.
---

# brain-refine

## Purpose

Keep the vault organised by actionability (PARA) and keep every link working (SB-1). Distilled notes in `brain/resources/` are the ones ADRs and skills must cite (SB-3), so refining is what makes knowledge reusable.

## When to use

- Several inbox notes cover the same topic (duplicates).
- Before writing an ADR or a skill that needs a distilled note to cite.
- At the end of every stage, before the stage review note (SB-2).
- When CI's brain validation fails.

## Steps

1. **Validate first.** Run `mvn test -Dtest=BrainVaultTest -Djacoco.skip=true` from the repository root and keep the list of problems.
2. **Group the inbox.** Read `brain/inbox/` and group notes by topic.
3. **Distil.** For each topic create or update one note in `brain/resources/` with `type: resource`, a summary, the rules/invariants, the project relevance (requirement IDs) and links back to its source notes. Credit the original authors in `authors`.
4. **Move processed raw notes** out of the inbox with `git mv` (so history is kept): reviewed research goes to `brain/research/`; inactive material goes to `brain/archive/`. Links keep working because Obsidian resolves `[[name]]` by file name.
5. **Fix schema problems.** Add the frontmatter from `brain-ingest` to any note without it. Take `authors` and `created` from `git log --follow --diff-filter=A -- <file>`, never guess.
6. **Fix broken links.** Rewrite title-style links such as `[[ACID Transactions]]` to the file name `[[acid-transactions]]`; delete links to notes that do not exist.
7. **Update** `brain/index.md` (list new distilled notes) and `brain/log.md` (what was merged or moved, by whom).
8. **Validate again** (step 1 command) until it passes, then open a pull request.

## Output

Inbox contains only unprocessed notes, distilled notes are in `brain/resources/`, the validator passes, and the log records the refinement.

## Example (Stage 3 run, 2026-11-27)

17 Stage 1 research notes were moved from `inbox/` to `research/` (they had already been distilled into 7 resource notes in Stage 2), 24 notes received frontmatter with authors from git history, and 18 broken title-style links were rewritten. Result recorded in `brain/log.md`.

## Safety

Do not delete another member's note: move it. Do not change who authored a note. Never add secrets.

## Grounding

Distillation follows the structure used in [[financial-reconciliation-and-auditability]] and [[immutable-ledger-and-audit-trails]].
