# ADR-001: Modular Monolith Architecture with Spring Boot

## Status
Accepted

## Date
2026-11-26

## Context
The project requires building a reliable digital wallet and double-entry financial ledger API supporting three distinct roles (User, Merchant, Admin) and handling concurrent financial transactions without data races or balance corruption. We evaluated two primary structural approaches:
1. **Microservices Architecture**: Separate services for Auth, Wallets, Transfers, Ledger, and Reconciliation communicating over HTTP/gRPC.
2. **Modular Monolith Architecture**: A single deployable Spring Boot application structured internally into clean, layered modules (Controller, Service, Repository) with strict domain boundaries.

Distributed microservices introduce complex distributed transaction management (Saga pattern, two-phase commit), network latency, partial failure modes, and distributed locking overhead. Given the project scope and single SQLite database design, microservices would add unnecessary operational friction.

## Decision
We adopt a **Modular Monolith** using Spring Boot 3.x with Java 17+. The application runs as a single process owning a single SQLite database file. 

The application is structured into strict internal modules:
- `auth`: User and merchant registration, JWT authentication.
- `wallet`: Wallet creation, retrieval, and status management.
- `transfer`: P2P transfers and merchant payments.
- `idempotency`: Idempotency key tracking and payload replay.
- `ledger`: **The single source of truth**—the only module authorized to insert ledger entries and update account balances.
- `reconciliation`: Administrative non-destructive audit engine.
- `admin`: Administrative reporting and management endpoints.

Modules interact via in-process Java interfaces, preventing circular dependencies and ensuring strict layer separation (Controller $\rightarrow$ Service $\rightarrow$ Repository).

## Consequences
- **Positive**:
  - Eliminates distributed transaction failures; all money operations execute within single ACID database transactions.
  - Zero network overhead for inter-module calls.
  - Simplified deployment and testing via local and in-memory databases.
- **Negative**:
  - Cannot scale individual modules independently (acceptable for academic and single-node demonstration targets).
  - Requires strict code reviews to ensure domain module boundaries and single-source-of-truth invariants are respected.

## Citations
- Second Brain Reference: [[double-entry-accounting-principles]], [[sqlite-concurrency-and-acid-transactions]]
- SRS Traceability: Section 2.1, NFR-MAIN-01, NFR-MAIN-02, Figure B-2
