---
title: "AI session 2026-11-27: ledger, idempotency, funding, reversals, reconciliation (M2)"
type: ai-session
authors: [Geethika M]
created: 2026-11-27
tags: [ai-session, stage-3]
requirements: [FR-LED-01, FR-LED-07, FR-WAL-07, FR-TRF-07, FR-REC-01, NFR-SAFE-01]
---

# AI session: ledger, idempotency, funding, reversals and reconciliation (Member 2 slice)

## Context

Same Claude Code session as the other slices (2026-11-27, repository owner's laptop). This slice was handed to Member 2 to review, run and commit from her own account. No secrets were used.

## Goal

Build the double-entry engine as the sole writer of entries and balances (ADR-003), the idempotency executor (ADR-005), admin test funding, reversals, reconciliation C1-C3 (ADR-007), admin ledger views and statistics; refine the brain (frontmatter schema, broken links) and add the CI brain validator.

## What worked

- Disassembling the sqlite-jdbc driver before relying on `BEGIN IMMEDIATE` revealed that its `commit()` immediately opens a new transaction; that shaped ADR-009 ([[sqlite-write-lock-and-driver-behaviour]]) and avoided a defect instead of fixing one later.
- Putting the idempotency lookup, the operation and the stored response in one write-locked transaction made scenario 7 ("database failure while writing") safe to retry; proven by failure injection in `RollbackFailureInjectionTest`.
- The brain validator found 24 notes without metadata and 18 broken links on its first run; the refine script fixed them using authors from git history.

## What failed or needed correction

- The first version of the transaction search built its SQL arguments in a confusing order; it was rewritten to be obviously correct before tests ran.
- A corruption test could have broken other tests' shared database; it now runs in its own Spring context and in a fixed order.

## Reviewer notes (Geethika to complete before opening the PR)


- [x] I read every file in my slice and can explain: the ledger engine, idempotency, funding, reversals and reconciliation C1-C3.
- [x] Questions or changes I made after review: strengthened the FAILED-transaction reversal test after Akshaya's review on PR #24.
- [x] `mvn -B verify` result on my machine: BUILD SUCCESS - 154 tests, 0 failures, 0 errors, 0 skipped.
