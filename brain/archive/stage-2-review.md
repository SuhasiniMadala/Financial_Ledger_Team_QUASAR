---
title: "Stage 2 review (Design) - written retrospectively"
type: review
authors: [Geethika M]
created: 2026-11-27
tags: [stage-2, review, sb-2]
---

# Stage 2 review (Design)

Written on 2026-11-27 at the start of Stage 3 (SB-2), because no review note was written when Stage 2 ended.

## What worked

- Seven ADRs, each citing a distilled note (SB-3), gave Stage 3 clear decisions to implement: modular monolith, SQLite WAL, double-entry immutable ledger, integer paise, idempotency keys, JWT, snapshot reconciliation.
- The ER design and DDL (`docs/architecture/database-schema.md`) were good enough to implement almost unchanged, including the immutability triggers.
- The sequence diagram for a transfer already put the write lock before the idempotency lookup - the ordering that prevents double charging.

## What did not work

- The design did not plan where FAILED transactions store their wallets: FAILED rows have no entries, so the schema needed `source_account_id` / `destination_account_id` columns (added in Stage 3, marked [S3] in `schema.sql`).
- TBD-1 (currencies) and TBD-2 (limit) were left open although the SRS asked for an ADR; recorded as ADR-008 in Stage 3.
- The traceability matrix had no Issue or PR columns and listed test classes that did not exist.
- The first `local-check` skill used an endpoint (`/actuator/health`) that differs from SRS SK-02 (`/api/v1/health`).
- ADR-002 said "BEGIN IMMEDIATE" without checking how the JDBC driver behaves; Stage 3 found a driver detail that needed ADR-009 ([[sqlite-write-lock-and-driver-behaviour]]).

## What to change

- Validate design assumptions with a tiny spike (one test) before accepting an ADR.
- Keep the traceability matrix in the same pull request as the change it traces.
