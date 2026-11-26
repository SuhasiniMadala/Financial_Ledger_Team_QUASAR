---
name: brain-refine
description: Instructions for refining, structuring, deduplicating, and moving notes from brain/inbox/ into distilled reference notes in brain/resources/, projects/, or areas/.
---

# Brain Refine Skill

## Purpose

The `brain-refine` skill guides team members and coding assistants in reviewing raw, newly ingested notes in `brain/inbox/`, deduplicating overlapping concepts, refining their technical insights, and distilling them into permanent PARA folders—primarily `brain/resources/` (for reference knowledge), `brain/areas/` (for long-term engineering standards), or `brain/projects/` (for stage-specific deliverables).

## When to Use

- When multiple raw notes in `inbox/` cover similar themes (e.g., SQLite concurrency, transactions, and locking).
- When transitioning from Stage 1 (Requirements & Ingestion) to Stage 2 (Design) or later stages.
- Before authoring Architecture Decision Records (ADRs) or technical designs, to ensure they cite synthesized, distilled knowledge.

## Procedure

1. **Scan `brain/inbox/`**:
   - Identify raw notes addressing a common domain (e.g. accounting mechanics, concurrency control, idempotency).
2. **Synthesize & Distill**:
   - Create or update a distilled note in `brain/resources/`.
   - Distill key concepts, mathematical invariants, architectural constraints, and citations into a structured, authoritative reference.
3. **Harmonize Links**:
   - Ensure all wikilinks resolve cleanly to either distilled resource notes or active project files.
   - Cross-link related resource notes.
4. **Preserve Provenance**:
   - Attribute original contributors and external authoritative sources.
5. **Update Second Brain Index & Log**:
   - Update `brain/index.md` with links to the new distilled resources.
   - Record the refinement action in `brain/log.md` detailing which notes were synthesized and where they now reside.

## Quality Checklist

- [ ] Clear title and concise summary.
- [ ] Explicit mathematical rules, schemas, or architectural constraints.
- [ ] Project-specific relevance tied to SRS requirements (e.g., `FR-1`, `TQ-6`).
- [ ] Bidirectional wikilinks: `[[double-entry-accounting-principles]]`.
- [ ] Log entry recorded in `brain/log.md`.
