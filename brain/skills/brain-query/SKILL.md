---
name: brain-query
description: Instructions for querying and synthesizing knowledge from the Second Brain vault to answer architectural, design, or requirements questions.
---

# Brain Query Skill

## Purpose

The `brain-query` skill provides a structured method for humans and AI agents to query the Second Brain knowledge base (`brain/`) to answer technical questions, clarify requirements, evaluate design alternatives, or draft documents (such as SRS sections, ADRs, or test plans) strictly using established knowledge notes.

## When to Use

- When making architectural or database design decisions that must adhere to agreed requirements.
- When drafting Architecture Decision Records (ADRs) in `docs/adr/`.
- During project reviews, audits, or stage transitions to trace design choices back to requirements and research.

## Procedure

1. **Identify the Core Question**:
   - Formulate the specific engineering question or topic (e.g., "How does SQLite serialize writes under WAL mode?").
2. **Search Brain Vault**:
   - Search across `brain/resources/`, `brain/areas/`, `brain/projects/`, and `brain/inbox/` for matching keywords, concept tags, and requirement IDs.
3. **Synthesize Brain Notes**:
   - Read the relevant notes.
   - Extract the agreed principles, constraints, mathematical formulas, and tradeoffs.
4. **Formulate the Answer**:
   - State the resolution or design recommendation clearly.
   - **Mandatory Attribution**: Explicitly cite every brain note consulted using wikilinks syntax (e.g., "As documented in [[double-entry-accounting-principles]]...").
   - Explicitly cite requirement IDs (e.g., `FR-1`, `DB-02`, `TQ-6`).

## Quality Checklist

- [ ] Uses only information grounded in the brain vault and project SRS.
- [ ] Cites every referenced note with wikilinks: `[[double-entry-accounting-principles]]`.
- [ ] Does not contradict immutable ledger invariants, double-entry rules, or SQLite constraints.
