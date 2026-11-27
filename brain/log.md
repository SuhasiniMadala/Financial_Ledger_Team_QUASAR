# Team Activity Log

This file records the team's progress, important decisions, research ingestions, and stage milestones.

## 21 November 2026

- Created the team GitHub repository.
- Added team members as collaborators.
- Created the brain folder.
- Started organizing research notes.

## 25 November 2026

### Research Contributions – Geethika

- Added research note on Double-Entry Bookkeeping.
- Added research note on ACID Transactions.
- Added research note on Idempotency in REST APIs.
- Added research note on Financial Ledger Reconciliation.

All four notes include summaries, sources, project relevance, and links to related concepts.

### Research Contributions – Suhasini

- Added research notes on database concurrency, locking, and SQLite WAL:
  - `database-concurrency.md`
  - `database-locking-and-race-conditions.md`
  - `sqlite-transactions-and-wal-mode.md`

## 26 November 2026

### Second Brain Organization & Contributions – Akshaya

- **PARA Reorganization**: Set up Second Brain folders (`inbox/`, `projects/`, `areas/`, `resources/`, `archive/`) following SDLC requirements.
- **Research Ingestion (brain-ingest)**: Ingested research notes into `brain/inbox/` covering immutable audit trails, minor units, API security, and agent skills.
- **Stage 1 Repository & CI Skeleton**: Established Maven build skeleton (Java 17, Spring Boot 3, SQLite JDBC), application properties, and GitHub Actions CI workflow.
- **Conflict Resolution**: Successfully merged `origin/main` into `akshaya/my-contribution`, harmonizing overlapping research notes and preserving all teammates' contributions without data loss.

### Stage 2 Architecture, Design & Second Brain Refinement – Team QUASAR

- **Skill Implementations**:
  - Implemented `brain-refine` (`brain/skills/brain-refine/SKILL.md`) for distilling raw inbox notes into PARA resources.
  - Implemented `brain-query` (`brain/skills/brain-query/SKILL.md`) for querying vault knowledge with mandatory citations.
  - Implemented initial `local-check` (`brain/skills/local-check/SKILL.md`) for standardizing local build, test, and health checks.
- **Second Brain Note Distillation (`brain/resources/`)**:
  - Distilled raw inbox notes into 7 authoritative reference notes:
    - `double-entry-accounting-principles.md`
    - `sqlite-concurrency-and-acid-transactions.md`
    - `api-idempotency-and-retry-semantics.md`
    - `immutable-ledger-and-audit-trails.md`
    - `financial-reconciliation-and-auditability.md`
    - `monetary-representation-and-integer-units.md`
    - `api-security-and-input-validation.md`
- **Architecture Decision Records (`docs/adr/`)**:
  - Authored ADR-001 through ADR-007, citing distilled resource notes and SRS requirements:
    - ADR-001: Modular Monolith Architecture with Spring Boot
    - ADR-002: SQLite Database with Write-Ahead Logging (WAL) and BEGIN IMMEDIATE Locking
    - ADR-003: Double-Entry Immutable Ledger as Single Source of Truth
    - ADR-004: Monetary Representation in Integer Minor Units (Paise)
    - ADR-005: Idempotency Key Pattern with SHA-256 Request Fingerprinting
    - ADR-006: Stateless Authentication via JWT and Role-Based Authorization
    - ADR-007: Non-Blocking Point-in-Time Ledger Reconciliation
- **System Architecture & UML Modeling (`docs/architecture/`)**:
  - Produced Modular Monolith Architecture document with Mermaid system topology and sequence flows (`system-architecture.md`).
  - Produced Component Design document with full domain class diagrams and collaboration flows (`component-design.md`).
- **Database & ER Design (`docs/architecture/`)**:
  - Produced complete Relational ER diagram, DDL definitions, check constraints, indexes, and immutability triggers (`database-schema.md`).
- **Engineering Planning (`docs/architecture/`)**:
  - Produced Development Plan and Task Breakdown across Stages 1 through 4 (`development-plan.md`).
  - Produced Requirements Traceability Matrix (`traceability-matrix.md`) mapping SRS requirements to ADRs, designs, tests, skills, and brain notes.

## 27 November 2026 - Stage 3 (Testing and CI)

### Audit and restart - Suhasini

- Audited the repository against the SDLC and SRS. The unreviewed `stage3-handoff` branch double-charged under concurrent retries; the team decided to delete it (local backup bundle kept) and rebuild Stage 3 from the end of Stage 2.
- AI session recorded in [[2026-11-27-suhasini-transfers-and-history]].

### brain-refine run - Geethika

- Moved 17 Stage 1 research notes from `inbox/` to `research/` (already distilled into resources in Stage 2).
- Added frontmatter (title, type, authors, created) to 24 notes; authors and dates taken from git history.
- Fixed 18 broken title-style wikilinks (e.g. `ACID Transactions` -> `acid-transactions`).
- Added the brain validator (`BrainVaultTest`) to CI; it reported all of the above before the fix.
- New distilled note [[sqlite-write-lock-and-driver-behaviour]]; area note [[testing-strategy-and-quality-gates]]; review [[stage-2-review]]; AI session [[2026-11-27-geethika-ledger-and-reconciliation]].

### Notes - Akshaya

- Area note [[api-error-contract-and-access-control]]; review [[stage-1-review]]; AI session [[2026-11-27-akshaya-accounts-and-platform]]; ADR-008.

### Notes - Suhasini

- Distilled note [[testing-concurrency-and-failure-injection]]; area [[ci-pipeline]]; project [[stage-3-testing-and-ci]]; review [[stage-3-review]]; ADR-010.

### Skill runs

- `local-check`, `simulate-transfer -Evaluate` and the brain skills were run for TQ-7; results in `docs/testing/skill-evaluations.md`.
- `brain-query` example answer recorded in `brain/skills/brain-query/SKILL.md`.
