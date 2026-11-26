# Team Activity Log

This file records the team's progress, important decisions, and research ingestions.

## 21 November 2026

- Created the team GitHub repository.
- Added team members as collaborators.
- Created the brain folder.
- Started organizing research notes.

## 26 November 2026

- **PARA Reorganization**: Set up Second Brain folders (`inbox/`, `projects/`, `areas/`, `resources/`, `archive/`) following SDLC guidelines.
- **Research Ingestion (brain-ingest)**: Ingested 10+ research notes into `brain/inbox/`:
  - `immutable-audit-trails.md` – Tamper-evident ledger, append-only entries, and reversal transactions (FR-4, DB-06). Contributed by Akshaya.
  - `integer-minor-units-and-monetary-precision.md` – Minor-unit representation in paise to prevent floating-point rounding errors (BR-01, TQ-6). Contributed by Akshaya.
  - `rest-api-security-and-testing.md` – API security practices, negative amount prevention, and testing tiers (NFR-SEC-03, TQ-4). Contributed by Akshaya.
  - `double-entry-bookkeeping.md` – Mathematical conservation of debits and credits, asset and liability accounts (FR-1, ACC-01–04). Contributed by Akshaya.
  - `idempotency-in-rest-apis.md` – Request fingerprinting, UUID idempotency keys, and retry replay mechanics (FR-2, FR-TRF-02–04). Contributed by Akshaya.
  - `acid-transactions-and-sqlite.md` – ACID transaction semantics, rollback mechanics, and SQLite durability (FR-3, NFR-SAFE-01). Contributed by Akshaya.
  - `database-concurrency-and-locking.md` – Race condition prevention and `BEGIN IMMEDIATE` transaction locking (FR-3, FR-CON-01–05). Contributed by Akshaya.
  - `sqlite-wal-mode-and-busy-timeout.md` – WAL mode configuration, non-blocking readers, and busy handler timeouts (DB-02, NFR-PERF-01). Contributed by Akshaya.
  - `financial-ledger-reconciliation.md` – Three-point reconciliation checks and read-only discrepancy reporting (FR-5, FR-REC-01–05). Contributed by Akshaya.
  - `jwt-authentication-and-role-based-authorization.md` – JWT token signing, environment secrets, and RBAC endpoint protection (NFR-SEC-01–03). Contributed by Akshaya.
  - `test-driven-development-and-concurrency-testing.md` – TDD practices, simulated rollback testing, and concurrency verification (TQ-1–TQ-5). Contributed by Akshaya.
  - `input-validation-and-boundary-checking.md` – Input sanitization, transaction upper/lower bounds, and negative amount rejection (BR-02, NFR-SEC-04). Contributed by Akshaya.
  - `agent-skills-for-system-verification.md` – Automated verification using repeatable agent skills (SK-01–SK-05). Contributed by Akshaya.
- **Stage 1 Repository & CI Skeleton**:
  - Established project build skeleton (Maven `pom.xml` with Java 17, Spring Boot 3.x, SQLite JDBC).
  - Configured GitHub Actions CI pipeline (`.github/workflows/ci.yml`) validating build, tests, secrets scanning, SKILL.md specification, and brain wikilinks.
  - Updated `AGENTS.md` with Second Brain conventions, SDLC requirements, and agent guidelines.
