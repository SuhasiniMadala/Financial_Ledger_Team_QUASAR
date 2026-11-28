# AGENTS.md

## Project
Financial Ledger – Team QUASAR  
Double-Entry Digital Wallet and Ledger API

## Team Members
- Suhasini M [BT2024043]
- Akshaya A [BT2024215]
- Geethika M [BT2024139]

## Overview
This repository contains the backend infrastructure for a digital wallet and financial ledger system.
The system implements a mathematically sound, tamper-evident, double-entry ledger ensuring that every credit has an equal and opposite debit, network retries never double-charge (idempotency), and concurrent requests never corrupt account balances.

Before implementing any feature or modifying any code or tests, agents and developers must consult:
- The project SRS (`Financial_Wallet_Ledger_SRS (2) (1).docx`)
- The SDLC and Workflow specification (`SDLC_and_Workflow.pdf`)
- The Project Statement

## General Instructions

1. Always refer to the SRS before implementing or changing any feature.
2. Do not introduce features or endpoints outside the agreed requirements without explicit team approval.
3. Write clean, modular, readable, and maintainable code following controller / service / repository layering.
4. Use meaningful variable, function, and class names.
5. Avoid unnecessary code duplication.
6. Handle invalid inputs, malformed IDs, and boundary errors properly with standard error responses.
7. Add clear explanatory comments wherever the implementation is not self-explanatory.
8. Test all changes against isolated test databases before considering any feature complete.

## Git & Team Guidelines

1. Never commit directly to `main`. Follow `docs/branching-strategy.md` (branches `stage3/member-<n>-<area>`, merge order M1 -> M2 -> M3).
2. Each member commits only their own work from their own Git identity (SB-6). Agents must never commit, push or open pull requests under another person's account.
3. Write meaningful commit messages and reference the issue (`Refs #12`, `Closes #12`).
4. Every change (code, skills, brain) goes through a pull request with at least one approving review by another member (SB-5).
5. Never commit passwords, tokens, API keys, or sensitive credentials. The JWT secret and admin password are environment variables only.
6. CI must be green on every pull request: build, lint, tests + coverage, secret scanning, SKILL.md validation, brain validation, local-check, and the frontend jobs (`docs/ci/ci-pipeline.md`).
7. The React client (`frontend/`, ADR-011) keeps money as integer paise, uses `useFinancialSubmit` for every money form, and never computes a balance itself.

## Commands agents and developers use

| Purpose | Command (repository root) |
|---|---|
| Full check before a PR | `mvn -B verify` |
| One test class | `mvn test -Dtest=TransferApiTest -Djacoco.skip=true` |
| Brain / skill validation only | `mvn test -Dtest=BrainVaultTest` / `mvn test -Dtest=SkillDefinitionsTest` (add `-Djacoco.skip=true`) |
| local-check skill | `powershell -ExecutionPolicy Bypass -File brain/skills/local-check/scripts/local-check.ps1` |
| simulate-transfer skill | see `brain/skills/simulate-transfer/SKILL.md` |
| React client: install, check, run | `cd frontend; npm ci; npm run lint; npm run typecheck; npm test; npm run dev` (backend on :8080) |
| Client against a real backend | `cd frontend; npm run test:integration` (see `frontend/tests/integration/README.md`) |

## Testing rules

- Never trust only an HTTP status for money: assert stored balances and end with reconciliation PASS.
- Every significant defect gets a regression test and a row in `docs/testing/defect-log.md` (TQ-8).
- Never delete, skip or weaken a failing test to get a green build; never report a result that was not run.

## Documentation & Second Brain (PARA)

1. Maintain the project knowledge base in `brain/` formatted as an Obsidian vault:
   - `brain/inbox/`: Unsorted, newly ingested research notes awaiting review.
   - `brain/projects/`: Active deliverables, tasks, and deadlines.
   - `brain/areas/`: Long-term standards, architecture guidelines, and continuous responsibilities.
   - `brain/resources/`: Distilled reference notes, domain guides, and cheatsheets.
   - `brain/archive/`: Inactive or historical records.
2. Maintain `brain/index.md` as the vault entry point and `brain/log.md` as the chronological activity log.
3. Use the `brain-ingest` skill to add research notes to `brain/inbox/`.
4. Link notes with wikilinks (e.g. `[[double-entry-accounting-principles]]`), requirement IDs (e.g., `FR-1`, `TQ-6`), and pull requests (SB-1).
5. Document significant technical decisions as Architecture Decision Records (ADRs) in `docs/adr/`, citing brain notes (SE-3, SB-3).
6. Record significant AI sessions in the brain as AI session notes detailing goals and outcomes.

## Development & Architecture Rules

- **Modular Monolith**: Spring Boot 3.x with Java 17+, owning a single SQLite database file.
- **Single Source of Truth**: Only the Ledger module may insert ledger entries or update wallet balances.
- **Double-Entry Invariant**: Every completed transaction must have equal debits and credits.
- **Integer Minor Units**: Money must strictly be handled as integer paise (minor units). Never use floating-point types (`float`, `double`) for money (TQ-6).
- **Concurrency & Locking**: Financial operations run in single SQLite transactions using `BEGIN IMMEDIATE` to serialize writes; WAL mode allows concurrent non-blocking reads.
- **Idempotency**: All mutating financial requests require an `Idempotency-Key` header; retried requests with the identical payload replay the stored response.
- **Immutable Ledger**: Committed ledger entries and transaction records are append-only. No UPDATE or DELETE operations; corrections must be explicit reversals.
