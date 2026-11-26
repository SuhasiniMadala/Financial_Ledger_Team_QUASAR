# Financial Ledger – Second Brain Knowledge Base

## Team QUASAR

This folder contains the complete Second Brain knowledge base for the Financial Ledger project, structured according to the **PARA** (Projects, Areas, Resources, Archive) methodology as defined in our SDLC requirements.

## Knowledge Base Structure (PARA)

The brain is organized by actionability:

- **`inbox/`** – Unsorted and newly ingested research notes awaiting review and distillation (managed via the `brain-ingest` skill).
- **`projects/`** – Active projects and milestones with concrete goals and deadlines (e.g., Stage 1 Requirements, Stage 2 Design).
- **`areas/`** – Ongoing domains of responsibility, quality standards, and continuous maintenance (e.g., Architecture, CI/CD, Security).
- **`resources/`** – Curated reference materials, distilled technical notes, and cheat sheets on financial systems, SQLite, and API design.
- **`archive/`** – Inactive notes, completed project stages, and historical milestones preserved for auditability.
- **`log.md`** – Chronological record of team activities, research ingestions, and project decisions.

## Ingested Research Notes (`inbox/`)

The following 10 research notes have been ingested and verified using the `brain-ingest` skill:

1. [[immutable-audit-trails]] – Immutable append-only audit trail and reversal transactions (FR-4, DB-06, NFR-SAFE-06).
2. [[integer-minor-units-and-monetary-precision]] – Integer paise and preventing floating-point rounding errors (BR-01, TQ-6).
3. [[rest-api-security-and-testing]] – Endpoint protection, negative amount rejection, and testing tiers (NFR-SEC-03, TQ-4).
4. [[double-entry-bookkeeping]] – Mathematical equality of debits and credits in financial ledgers (FR-1, ACC-01–04).
5. [[idempotency-in-rest-apis]] – Duplicate charge prevention via UUID keys and request fingerprinting (FR-2, FR-TRF-02–04).
6. [[acid-transactions-and-sqlite]] – ACID properties, transaction atomicity, and single-database durability (FR-3, DB-01, NFR-SAFE-01).
7. [[database-concurrency-and-locking]] – Managing race conditions and concurrent balance mutability via `BEGIN IMMEDIATE` (FR-3, FR-CON-01–05).
8. [[sqlite-wal-mode-and-busy-timeout]] – Non-blocking concurrent readers, WAL configuration, and busy timeouts (DB-02, NFR-PERF-01).
9. [[financial-ledger-reconciliation]] – Independent auditing endpoint verifying mathematical ledger consistency (FR-5, FR-REC-01–05).
10. [[jwt-authentication-and-role-based-authorization]] – JWT tokens, secret management, and role/ownership security (FR-AUTH-01–06, NFR-SEC-01–03).
11. [[test-driven-development-and-concurrency-testing]] – Layered testing (unit, rollback integration, concurrency, system) (TQ-1–TQ-5, NFR-TEST-01–05).
12. [[input-validation-and-boundary-checking]] – Request parameter boundaries, negative amount prevention, and sanitization (BR-02, NFR-SEC-04).
13. [[agent-skills-for-system-verification]] – Operational verification via standardized `SKILL.md` workflows (SK-01–SK-05).

## Team Members

- **Suhasini M** [BT2024043]
- **Akshaya A** [BT2024215]
- **Geethika M** [BT2024139]

## Project Goal

To design and build a robust, mathematically sound digital wallet backend backed by a strict, tamper-evident double-entry ledger that guarantees ACID transactions, data integrity, idempotency, and clean API design.
