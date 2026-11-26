# Financial Ledger – Second Brain Knowledge Base

## Team QUASAR

This folder contains the complete Second Brain knowledge base for the Financial Ledger project, structured according to the **PARA** (Projects, Areas, Resources, Archive) methodology as defined in our SDLC requirements.

## Knowledge Base Structure (PARA)

The brain is organized by actionability:

- **`inbox/`** – Unsorted and newly ingested research notes awaiting review and distillation (managed via the `brain-ingest` skill).
- **`projects/`** – Active projects, stage deliverables, and milestones (e.g., Stage 1 Requirements, Stage 2 Design).
- **`areas/`** – Ongoing domains of responsibility, quality standards, and continuous maintenance (e.g., Architecture, CI/CD, Security).
- **`resources/`** – Curated reference materials, distilled technical notes, and cheat sheets on financial systems, SQLite, and API design.
- **`archive/`** – Inactive notes, completed project stages, and historical milestones preserved for auditability.
- **`skills/`** – Executable agent workflows (`brain-ingest`, `brain-refine`, `brain-query`, `local-check`).
- **`decisions/`** – Team meeting decisions and summaries.
- **`meetings/`** – Records of team discussions.
- **`research/`** – Reviewed technical research repository.
- **`log.md`** – Chronological record of team activities, research ingestions, and project decisions.

## Distilled Reference Notes (`brain/resources/`)

The following distilled notes synthesize core technical principles and serve as citations for Architecture Decision Records (ADRs):

1. [[double-entry-accounting-principles]] – Double-entry accounting equation, asset vs. liability account mechanics, and conservation invariants (cited by ADR-001, ADR-003).
2. [[sqlite-concurrency-and-acid-transactions]] – SQLite file locking, `BEGIN IMMEDIATE` serialization, and WAL mode (cited by ADR-001, ADR-002, ADR-003, ADR-007).
3. [[api-idempotency-and-retry-semantics]] – Idempotency key pattern, SHA-256 request hashing, and retry replay mechanics (cited by ADR-005).
4. [[immutable-ledger-and-audit-trails]] – Append-only ledger entries, DB triggers, and explicit reversal transactions (cited by ADR-003).
5. [[financial-reconciliation-and-auditability]] – Three-point reconciliation checks (C1, C2, C3) and non-blocking snapshot audits (cited by ADR-003, ADR-007).
6. [[monetary-representation-and-integer-units]] – Minor units in paise, integer arithmetic, and eliminating floating-point errors (cited by ADR-004).
7. [[api-security-and-input-validation]] – JWT authentication, role-based authorization, ownership isolation, and parameter bounds (cited by ADR-006).

## Ingested Research Notes (`brain/inbox/`)

The inbox contains research notes contributed by team members:
- `immutable-audit-trails.md`
- `integer-minor-units-and-monetary-precision.md`
- `rest-api-security-and-testing.md`
- `double-entry-bookkeeping.md`
- `idempotency-in-rest-apis.md`
- `acid-transactions-and-sqlite.md`
- `acid-transactions.md`
- `database-concurrency-and-locking.md`
- `database-concurrency.md`
- `database-locking-and-race-conditions.md`
- `sqlite-wal-mode-and-busy-timeout.md`
- `sqlite-transactions-and-wal-mode.md`
- `financial-ledger-reconciliation.md`
- `jwt-authentication-and-role-based-authorization.md`
- `test-driven-development-and-concurrency-testing.md`
- `input-validation-and-boundary-checking.md`
- `agent-skills-for-system-verification.md`

## Team Members

- **Suhasini M** [BT2024043]
- **Akshaya A** [BT2024215]
- **Geethika M** [BT2024139]

## Project Goal

To design and build a robust, mathematically sound digital wallet backend backed by a strict, tamper-evident double-entry ledger that guarantees ACID transactions, data integrity, idempotency, and clean API design.
