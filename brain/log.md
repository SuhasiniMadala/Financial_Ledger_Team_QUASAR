# Team Activity Log

This file records the team's progress, important decisions, and research ingestions.

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
- **Research Ingestion (brain-ingest)**: Ingested research notes into `brain/inbox/`:
  - `immutable-audit-trails.md` – Tamper-evident ledger, append-only entries, and reversal transactions (FR-4, DB-06).
  - `integer-minor-units-and-monetary-precision.md` – Minor-unit representation in paise to prevent floating-point rounding errors (BR-01, TQ-6).
  - `rest-api-security-and-testing.md` – API security practices, negative amount prevention, and testing tiers (NFR-SEC-03, TQ-4).
  - `double-entry-bookkeeping.md` – Harmonized and synthesized core accounting principles, mathematical conservation of debits and credits.
  - `idempotency-in-rest-apis.md` – Harmonized request fingerprinting, UUID keys, and replay mechanics (FR-2, FR-TRF-02–04).
  - `acid-transactions-and-sqlite.md` – SQLite ACID transaction semantics, rollback mechanics, and durability (FR-3, NFR-SAFE-01).
  - `database-concurrency-and-locking.md` – Race condition prevention and `BEGIN IMMEDIATE` transaction locking (FR-3, FR-CON-01–05).
  - `sqlite-wal-mode-and-busy-timeout.md` – WAL mode configuration, non-blocking readers, and busy handler timeouts (DB-02, NFR-PERF-01).
  - `financial-ledger-reconciliation.md` – Harmonized three-point reconciliation checks and read-only discrepancy reporting (FR-5, FR-REC-01–05).
  - `jwt-authentication-and-role-based-authorization.md` – JWT token signing, environment secrets, and RBAC endpoint protection (NFR-SEC-01–03).
  - `test-driven-development-and-concurrency-testing.md` – TDD practices, simulated rollback testing, and concurrency verification (TQ-1–TQ-5).
  - `input-validation-and-boundary-checking.md` – Input sanitization, transaction upper/lower bounds, and negative amount rejection (BR-02, NFR-SEC-04).
  - `agent-skills-for-system-verification.md` – Automated verification using repeatable agent skills (SK-01–SK-05).
- **Stage 1 Repository & CI Skeleton**:
  - Established project build skeleton (Maven `pom.xml` with Java 17, Spring Boot 3.x, SQLite JDBC).
  - Configured GitHub Actions CI pipeline (`.github/workflows/ci.yml`) validating lint, build, tests, secrets scanning, SKILL.md specification, and brain wikilinks.
  - Updated `AGENTS.md` with Second Brain conventions, SDLC requirements, and agent guidelines.
