# Financial Ledger - Second Brain (Team QUASAR)

Entry point of the Obsidian vault. Organised by actionability (PARA, SDLC section 1). Every note carries frontmatter (title, type, authors, created); CI checks the schema and every `[[wikilink]]` (`BrainVaultTest`).

## How to use this vault

- Add knowledge with the `brain-ingest` skill (`brain/skills/brain-ingest/SKILL.md`) -> lands in `inbox/`.
- Organise and distil with `brain-refine` -> `resources/`, `research/`, `archive/`.
- Ask questions or draft documents with `brain-query`, always citing notes.
- Operational skills: `local-check` (SK-02) and `simulate-transfer` (SK-01).
- Every change goes through a pull request (SB-5); `brain/log.md` records every run.

## Projects (active)

- [[stage-3-testing-and-ci]] - Stage 3 plan, owners and deliverables
- [[stage-3-review]] - Stage 3 review note (SB-2)
- [[stage-4-client-and-review]] - Stage 4 plan, owners and deliverables (React client, live review, release)
- AI-session notes (SDLC 6): [[2026-11-27-akshaya-accounts-and-platform]], [[2026-11-27-geethika-ledger-and-reconciliation]], [[2026-11-27-suhasini-transfers-and-history]]

## Areas (ongoing standards)

- [[api-error-contract-and-access-control]] - error format, 404-not-403, endpoint checklist
- [[testing-strategy-and-quality-gates]] - what must pass before a merge; defect workflow (TQ-8)
- [[ci-pipeline]] - what each CI job proves

## Resources (distilled - cite these from ADRs and skills, SB-3)

1. [[double-entry-accounting-principles]] - debits = credits, asset vs liability accounts (ADR-001, ADR-003, ADR-008)
2. [[sqlite-concurrency-and-acid-transactions]] - SQLite locking, WAL, ACID (ADR-002, ADR-009, ADR-010)
3. [[sqlite-write-lock-and-driver-behaviour]] - BEGIN IMMEDIATE in practice, busy timeout, sqlite-jdbc details (ADR-009) - Stage 3
4. [[api-idempotency-and-retry-semantics]] - idempotency keys and replay (ADR-005)
5. [[immutable-ledger-and-audit-trails]] - append-only entries, reversals (ADR-003)
6. [[financial-reconciliation-and-auditability]] - checks C1-C3 (ADR-007)
7. [[monetary-representation-and-integer-units]] - integer paise (ADR-004, ADR-008)
8. [[api-security-and-input-validation]] - JWT, RBAC, validation (ADR-006)
9. [[testing-concurrency-and-failure-injection]] - how we test money code (ADR-010) - Stage 3

## Research (reviewed raw notes, distilled above)

Stage 1 research by all three members, moved here from `inbox/` by `brain-refine` on 2026-11-27:
[[acid-transactions]], [[acid-transactions-and-sqlite]], [[agent-skills-for-system-verification]], [[database-concurrency]], [[database-concurrency-and-locking]], [[database-locking-and-race-conditions]], [[double-entry-bookkeeping]], [[financial-ledger-reconciliation]], [[idempotency-in-rest-apis]], [[immutable-audit-trails]], [[input-validation-and-boundary-checking]], [[integer-minor-units-and-monetary-precision]], [[jwt-authentication-and-role-based-authorization]], [[rest-api-security-and-testing]], [[sqlite-transactions-and-wal-mode]], [[sqlite-wal-mode-and-busy-timeout]], [[test-driven-development-and-concurrency-testing]]

## Archive (inactive)

- [[stage-1-review]] - Stage 1 review (written retrospectively)
- [[stage-2-review]] - Stage 2 review (written retrospectively)

## Other folders

- `inbox/` - new, unsorted notes (currently empty after refinement)
- `decisions/`, `meetings/` - team decisions and meeting notes
- `skills/` - the five Agent Skills

## Team

Suhasini M [BT2024043], Akshaya A [BT2024215], Geethika M [BT2024139]
