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
