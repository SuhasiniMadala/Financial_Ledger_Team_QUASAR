# Changelog

All notable changes to this project. Format based on "Keep a Changelog"; versions follow `v<major>.<minor>.<patch>-stage<N>` (see `docs/branching-strategy.md`).

## [Unreleased]

Stage 4 (React client and final demonstration) - nothing yet.

## v0.3.0-stage3 - 2026-11-27 (Stage 3: Testing and CI)

Merged in PRs #23 (M1 Akshaya), #24 (M2 Geethika), #25 and #26 (M3 Suhasini), #27 and #28 (CI results and review notes), then tagged `v0.3.0-stage3` with CI green on `main`.

### Added
- Complete REST API from SRS Table 3-1: registration and login with JWT, wallets, admin directory and wallet status, idempotent test funding, transfers, merchant payments, reversals, reconciliation, statistics, user and merchant views, `GET /api/v1/health` (M1 Akshaya, M2 Geethika, M3 Suhasini).
- Double-entry ledger engine as the sole writer of entries and balances; immutability triggers; FAILED transactions for rejected operations.
- `BEGIN IMMEDIATE` transaction manager (ADR-009); `503 SERVICE_BUSY` with `Retry-After` on write-lock timeout.
- 153 automated tests: unit, integration, API, security matrix, concurrency, failure injection, busy lock, durability (kill and restart), OpenAPI coverage, architecture rules, brain and skill validators; JaCoCo rule (>= 70% on the financial modules; 96.5% overall).
- Performance benchmark for NFR-PERF-01..03 (manual run, results in `docs/testing/test-execution-report.md`).
- Skills: `local-check` script (SK-02) and `simulate-transfer` script with evaluation cases (SK-01); brain skills updated.
- CI on every pull request: build, Checkstyle lint, tests + coverage, gitleaks, SKILL.md validation, brain validation, local-check.
- ADR-008 (currencies INR/USD and per-transaction limit), ADR-009, ADR-010.
- Documentation: README, user guide, branching strategy, module docs, test plan, test execution report, defect log, skill evaluations, CI plan, traceability matrix with issues and PRs, OpenAPI spec and Postman collection.
- Brain: frontmatter schema, 9 new notes (every member), Stage 1-3 review notes, AI-session notes.

### Changed
- Brain: 17 distilled Stage 1 research notes moved from `inbox/` to `research/`; 18 broken wikilinks fixed.
- `local-check` now calls `GET /api/v1/health` as required by SRS SK-02 (was `/actuator/health`).
- Spring Boot Actuator removed (health is the SRS endpoint, which reads the database).

### Fixed
- DEF-001 .. DEF-006, each with a regression test or evaluation (`docs/testing/defect-log.md`).

### Removed
- The unreviewed `stage3-handoff` branch (deleted from GitHub at the team's request after the audit; a backup bundle was kept locally).

## v0.2.0 - Stage 2: Design (merged in PR #4, 2026-11-26, no tag)
- Architecture, component, database and sequence designs; ADR-001..ADR-007; brain-refine, brain-query, first local-check skill; 7 distilled notes; development plan; traceability matrix.

## v0.1.0 - Stage 1: Requirements (merged in PRs #1-#3, 2026-11-21..26, no tag)
- SRS v1.2; brain vault (PARA, index, log); 17 research notes by all members; brain-ingest skill; AGENTS.md; Maven skeleton and CI skeleton.
