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

1. Do not commit directly to `main` for major features or changes.
2. Work on dedicated feature branches (e.g., `akshaya/my-contribution` or `feature/<name>`).
3. Write meaningful, conventional commit messages.
4. Review all changes through pull requests before merging. Every PR requires at least one review.
5. Never commit passwords, tokens, API keys, or sensitive credentials to the repository.
6. CI pipelines must pass on every pull request (build, test, secret scanning, SKILL.md validation, brain note validation).

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
