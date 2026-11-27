---
name: brain-query
description: Answer an engineering question about the Financial Ledger, or draft a document such as an ADR, SRS section or test plan, using only knowledge recorded in the brain vault and the SRS, with a wikilink citation for every note used. Use during design, reviews, and the Stage 4 live demonstration.
---

# brain-query

## Purpose

Make answers traceable: every claim points to a note, a requirement ID, and - where relevant - an ADR, code file or test (the Stage 4 walk-through "brain note -> ADR -> code -> test").

## Inputs

A specific question, for example "Why does a transfer take the write lock before reading the idempotency key?"

## Steps

1. **Restate the question** and list the requirement IDs it touches (search the SRS and `docs/architecture/traceability-matrix.md`).
2. **Search the vault**: start at `brain/index.md`, then search `brain/resources/` (distilled, preferred), `brain/areas/`, `brain/projects/` and `brain/research/` for keywords and requirement IDs.
3. **Read** the matching notes and the ADRs that cite them (`docs/adr/`).
4. **Answer** in plain language. Cite every note used as a wikilink, plus requirement IDs, ADR numbers and the code/test file that implements it.
5. **Say what is not covered.** If the vault does not answer the question, state that and suggest a `brain-ingest` task instead of guessing.
6. If the answer is worth keeping, save it as a note (via `brain-ingest`).

## Output format

```
Question: ...
Answer: ...
Evidence: [[note-name]] (requirement IDs) -> ADR-00X -> src/... -> src/test/...
Gaps: ...
```

## Example (run 2026-11-27)

Question: why must the idempotency lookup happen after BEGIN IMMEDIATE?
Answer: if two identical requests both read "no key yet" before either holds the write lock, both execute and the user is charged twice. Taking the single SQLite write lock first makes the second request wait, then find the stored key and replay it.
Evidence: [[api-idempotency-and-retry-semantics]], [[sqlite-concurrency-and-acid-transactions]], [[sqlite-write-lock-and-driver-behaviour]] (FR-TRF-07, FR-CON-02, FR-CON-08) -> ADR-005, ADR-009 -> `IdempotencyService.java`, `SqliteImmediateTransactionManager.java` -> `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce`.

## Quality checklist

- Every statement is backed by a cited note, requirement or file.
- Nothing contradicts the double-entry, immutability or integer-money rules in [[double-entry-accounting-principles]] and [[monetary-representation-and-integer-units]].
- Gaps are stated, not filled with guesses.
