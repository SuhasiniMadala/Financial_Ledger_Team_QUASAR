# Architecture Decision Records (ADRs)

This directory contains records of significant architectural, technical, and design decisions made for the Financial Ledger project, as mandated by SE-3 of the course SDLC.

Every ADR documents:
- **Status**: Proposed, Accepted, Deprecated, or Superseded.
- **Context**: The problem statement and background.
- **Decision**: The chosen technical approach.
- **Consequences**: Positive and negative outcomes or tradeoffs.
- **Citations**: Explicit references to distilled Second Brain resource notes (`brain/resources/`) and SRS requirements.

## Index of Decisions

- [ADR-001: Modular Monolith Architecture with Spring Boot](ADR-001-modular-monolith-architecture.md)
- [ADR-002: SQLite Database with Write-Ahead Logging (WAL) and BEGIN IMMEDIATE Locking](ADR-002-sqlite-wal-and-locking.md)
- [ADR-003: Double-Entry Immutable Ledger as Single Source of Truth](ADR-003-double-entry-immutable-ledger.md)
- [ADR-004: Monetary Representation in Integer Minor Units (Paise)](ADR-004-monetary-representation-integer-minor-units.md)
- [ADR-005: Idempotency Key Pattern with SHA-256 Request Fingerprinting](ADR-005-idempotency-key-pattern.md)
- [ADR-006: Stateless Authentication via JWT and Role-Based Authorization](ADR-006-stateless-jwt-auth-rbac.md)
- [ADR-007: Non-Blocking Point-in-Time Ledger Reconciliation](ADR-007-non-blocking-ledger-reconciliation.md)
- [ADR-008: Supported Currencies (TBD-1) and Per-Transaction Limit (TBD-2)](ADR-008-supported-currencies-and-transaction-limit.md) - Stage 3, Akshaya A
- [ADR-009: Explicit BEGIN IMMEDIATE in a Custom Transaction Manager (refines ADR-002)](ADR-009-explicit-begin-immediate-transaction-manager.md) - Stage 3, Geethika M
- [ADR-010: A Fresh SQLite File per Test Context](ADR-010-test-database-isolation.md) - Stage 3, Suhasini M

CI checks SB-3 for every ADR: `BrainVaultTest.everyAdrCitesADistilledNote` fails if an ADR cites no note from `brain/resources/`.
