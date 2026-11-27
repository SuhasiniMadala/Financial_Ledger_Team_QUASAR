---
name: brain-ingest
description: Add a new research note or summary to the Financial Ledger second brain (brain/inbox/) with the required frontmatter schema, wikilinks to related notes and requirement IDs, and a log entry. Use when a team member or agent has read a source worth keeping.
---

# brain-ingest

## Purpose

Capture new knowledge (articles, documentation, experiment results, AI-session outcomes) in `brain/inbox/` so that it can later be distilled by `brain-refine` and cited by ADRs and skills (SB-1, SB-3). Every note added this way is reviewed in a pull request (SB-5) and is visible in the author's commit history (SB-6).

## Inputs

- The source: a URL, a document, or the outcome of a session.
- The author's name as used in the team (for example `Geethika M`).
- The requirement IDs it relates to (for example `FR-2`, `FR-CON-02`, `TQ-6`).

## Steps

1. Create `brain/inbox/<short-kebab-case-name>.md`. Check first (search `brain/`) that no note already covers the same topic; if one does, extend it instead of duplicating it.
2. Start the file with this frontmatter (the CI brain validator, `BrainVaultTest`, rejects notes without it):

   ```yaml
   ---
   title: "Human readable title"
   type: research            # research | resource | project | area | archive | review | ai-session | decision | meeting
   authors: [Your Name]
   created: 2026-11-27       # yyyy-MM-dd
   tags: [sqlite, concurrency]        # optional
   requirements: [FR-CON-02, DB-02]   # optional
   ---
   ```

3. Write the body: `# Title`, a 3-5 line **Summary** in your own words, **Key points**, **Relevance to the project** (name the requirement IDs), **Source** (URL), and **Related notes**.
4. Link related notes with wikilinks written as the note's file name, for example `[[sqlite-concurrency-and-acid-transactions]]`. Never write a link to a note that does not exist: the CI brain check fails on broken links.
5. Add a dated line to `brain/log.md`: date, note, author, one-line description.
6. Commit on your own branch, open a pull request, and ask another member to review it.

## Output

One new note in `brain/inbox/`, one new line in `brain/log.md`, and a passing `mvn test -Dtest=BrainVaultTest -Djacoco.skip=true`.

## Failure handling

- Validator reports "missing YAML frontmatter" or a bad field: fix the header exactly as in step 2.
- Validator reports a broken wikilink: correct the file name, or remove the link.
- The topic already exists: append to the existing note and record that in the log instead.

## Safety

Never paste secrets, tokens, passwords or real personal or financial data into a note (SDLC 6). Summarise sources in your own words.

## Grounding

The note format and linking rules follow the distilled guidance in [[double-entry-accounting-principles]] and [[sqlite-concurrency-and-acid-transactions]], which were produced with this skill and then refined. The decision to keep raw research separate from distilled resources is described in `brain-refine`.
