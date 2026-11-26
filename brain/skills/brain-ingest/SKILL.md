---
name: brain-ingest
description: Instructions for adding, summarising, linking, and maintaining research notes in the Financial Ledger project's knowledge base.
---

# Brain Ingest Skill

## Purpose

This skill defines how team members add research and technical knowledge to the project's brain folder.

## Adding a New Research Note

1. Create the research note inside `brain/inbox/`.
2. Give the file a short, descriptive name.
3. Add a clear title to the note.
4. Summarise the research source in 3–5 meaningful lines.
5. Include the original source URL.
6. Link related research notes using `[[note-name]]` syntax.
7. Explain how the research is relevant to the Financial Ledger project.
8. Add a dated entry to `brain/log.md`.

## Note Format

Every research note should contain:

- Title
- Summary (3–5 lines)
- Source link
- Related notes using `[[links]]`
- Relevance to the project

## Linking Notes

Use wiki-style links to connect related concepts.

Example:

A note about ACID transactions can link to:

- [[sqlite-wal-mode]]
- [[sqlite-locking]]
- [[concurrency-race-conditions]]

## Logging

After adding a note, update `brain/log.md` with:

- Date
- Note added
- Contributor
- Brief description

## Guidelines

- Use reliable technical documentation and research sources.
- Write in your own words.
- Avoid unsupported claims.
- Keep notes concise and technically accurate.
- Do not duplicate existing notes.
- Submit every research note through a pull request.
- Request a review from another team member before merging.
