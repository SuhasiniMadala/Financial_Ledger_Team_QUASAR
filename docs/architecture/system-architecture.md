# System Architecture and Component Design

## 1. System Overview and Perspective

The Financial Ledger system is designed as a **Modular Monolith** running as a single Java 17 Spring Boot 3 process that exclusively owns a local SQLite database file operating in Write-Ahead Logging (WAL) mode. Clients connect over HTTP/1.1 with JSON payloads.

```mermaid
graph TD
    subgraph Clients["Client Tier"]
        UI["React Web Interface (User / Merchant / Admin)"]
        TestClient["Automated Tests & Skills (simulate-transfer, local-check, Postman)"]
    end

    subgraph AppTier["Application Tier — Spring Boot 3 Modular Monolith"]
        Security["Spring Security Filter Chain (JWT Auth & Role Authorization)"]
        Controllers["REST API Controllers (/api/v1)"]
        
        subgraph DomainModules["Domain Modules (Service Layer)"]
            AuthModule["Auth & Accounts Module"]
            WalletModule["Wallet Management Module"]
            TransferModule["Transfer & Payment Module"]
            IdemModule["Idempotency Module"]
            ReconModule["Reconciliation Module"]
            AdminModule["Admin & Reporting Module"]
            LedgerModule["Ledger Module (SOLE WRITER)"]
        end
        
        RepoLayer["Repository Layer (Spring JDBC / SQLite)"]
    end

    subgraph DataTier["Data Tier"]
        DB[(SQLite Database File: WAL Mode + synchronous=FULL)]
    end

    UI -->|HTTP / JSON + Bearer JWT + Idempotency-Key| Security
    TestClient -->|HTTP / JSON + Bearer JWT + Idempotency-Key| Security
    Security --> Controllers
    Controllers --> DomainModules
    
    AuthModule --> RepoLayer
    WalletModule --> LedgerModule
    TransferModule --> IdemModule
    TransferModule --> LedgerModule
    ReconModule --> RepoLayer
    AdminModule --> LedgerModule
    AdminModule --> RepoLayer
    
    LedgerModule -->|BEGIN IMMEDIATE ... COMMIT / ROLLBACK| RepoLayer
    RepoLayer --> DB
```

## 2. Module Boundaries and Responsibilities

Following requirement `NFR-MAIN-01` and `NFR-MAIN-02`, the architecture is structured into layered modules with unidirectional dependencies:

| Module | Primary Interface / Class | Responsibilities & Invariants |
|---|---|---|
| **`auth`** | `AuthService`, `JwtTokenProvider` | Handles registration, credential validation with BCrypt, merchant code generation, and signed JWT issuance. |
| **`wallet`** | `WalletService` | Manages wallet creation (one per owner per currency), balance reading, and administrative status transitions (`ACTIVE`, `FROZEN`, `DEACTIVATED`). |
| **`transfer`** | `TransferService`, `PaymentService` | Validates input bounds, enforces same-currency rules, verifies wallet active status, and coordinates transfers and merchant payments. |
| **`idempotency`**| `IdempotencyService` | Computes SHA-256 payload hashes, retrieves stored responses for replays, and persists idempotency records within financial transactions. |
| **`ledger`** | `LedgerService` | **The Single Source of Truth**. Sole writer of ledger entries and cached wallet balances. Enforces $\sum \text{Debits} = \sum \text{Credits}$ before committing. |
| **`reconciliation`**| `ReconciliationService`| Executes read-only point-in-time audits: verifies C1 (currency totals), C2 (transaction internal balance), and C3 (account balance derivation). |
| **`admin`** | `AdminService` | Admin dashboard statistics, system funding, and transaction reversals. |

## 3. Financial Transaction Lifecycle Flow

All balance mutations run inside a single SQLite transaction with `BEGIN IMMEDIATE` to guarantee race safety.

```mermaid
sequenceDiagram
    autonumber
    actor Client as User / Merchant / Admin
    participant Controller as REST Controller
    participant Service as Transfer / Payment Service
    participant Idem as Idempotency Module
    participant Ledger as Ledger Module (Sole Writer)
    participant DB as SQLite (WAL Mode)

    Client->>Controller: POST /api/v1/transfers (with Idempotency-Key & JWT)
    Controller->>Service: executeTransfer(transferRequest, principalId, idemKey)
    Service->>DB: BEGIN IMMEDIATE (Acquire write lock)
    alt Write lock timeout (> 5000ms)
        DB-->>Service: SQLITE_BUSY
        Service-->>Controller: ServiceBusyException
        Controller-->>Client: 503 SERVICE_BUSY (Retry-After: 1)
    end

    Service->>Idem: checkKey(principalId, idemKey, requestHash)
    alt Key already used with same hash
        Idem-->>Service: ReplayStoredResponse(cachedStatus, cachedBody)
        Service->>DB: ROLLBACK (read-only)
        Service-->>Controller: Return Replay
        Controller-->>Client: Replayed Response (Idempotent-Replayed: true)
    else Key reused with different hash
        Service->>DB: ROLLBACK
        Service-->>Controller: KeyConflictException
        Controller-->>Client: 409 IDEMPOTENCY_KEY_REUSED
    end

    Service->>Service: Validate balances & business rules (active, same currency)
    alt Insufficient funds or invalid state
        Service->>DB: Record FAILED transaction & Save Idempotency Key
        Service->>DB: COMMIT
        Service-->>Controller: BusinessRuleException (e.g. INSUFFICIENT_FUNDS)
        Controller-->>Client: 422 Unprocessable Entity
    else Business rules pass
        Service->>Ledger: postTransfer(sourceWallet, destWallet, amount)
        Ledger->>DB: INSERT INTO transaction_record (status=COMPLETED)
        Ledger->>DB: INSERT INTO ledger_entry (DEBIT source, CREDIT dest)
        Ledger->>DB: UPDATE account SET balance = balance - amount (source)
        Ledger->>DB: UPDATE account SET balance = balance + amount (dest)
        Ledger->>Ledger: Assert sum(Debits) == sum(Credits)
        Ledger->>Idem: recordOutcome(principalId, idemKey, 201, responseBody)
        Ledger->>DB: COMMIT (Releases write lock)
        Ledger-->>Service: TransferCompletedDTO
        Service-->>Controller: 201 Created
        Controller-->>Client: 201 CREATED (transactionId, balances)
    end
```

## 4. Reconciliation Engine Flow

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin Client
    participant ReconCtrl as ReconciliationController
    participant ReconService as ReconciliationService
    participant DB as SQLite (WAL Mode)

    Admin->>ReconCtrl: POST /api/v1/admin/reconciliations
    ReconCtrl->>ReconService: runReconciliation()
    ReconService->>DB: BEGIN (Read-only Snapshot)
    Note over ReconService,DB: Reads consistent WAL snapshot without blocking active writers
    ReconService->>DB: Check C1: SELECT currency, sum(debit), sum(credit) FROM ledger_entry GROUP BY currency
    ReconService->>DB: Check C2: SELECT transaction_id, sum(debit), sum(credit), count(*) FROM ledger_entry GROUP BY transaction_id
    ReconService->>DB: Check C3: Compare cached balance in account table with calculated ledger sums (credits - debits for Wallets; debits - credits for System Funding)
    ReconService->>DB: COMMIT (End Read Transaction)
    Note over ReconService: Zero mutation; pure read audit
    alt All checks match exactly
        ReconService-->>ReconCtrl: ReconciliationReport(status=PASS, discrepancies=[])
        ReconCtrl-->>Admin: 200 OK (Report: PASS)
    else Any mismatch detected
        ReconService-->>ReconCtrl: ReconciliationReport(status=FAIL, discrepancies=[...])
        ReconCtrl-->>Admin: 200 OK (Report: FAIL with detailed diffs)
    end
```
