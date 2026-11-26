# Requirements Traceability Matrix (RTM)

## Complete End-to-End Traceability Matrix

This matrix establishes end-to-end traceability across the software development lifecycle:
$$\text{Requirement} \longrightarrow \text{Design / Architecture} \longrightarrow \text{ADR} \longrightarrow \text{Test / Quality} \longrightarrow \text{Agent Skill} \longrightarrow \text{Second Brain Note}$$

| Req ID | Requirement Description | Design Document / Component | ADR | Test / Verification (TQ) | Agent Skill | Distilled Brain Note |
|---|---|---|---|---|---|---|
| **FR-1** | **Wallet Initialization**: Accounts backed by double-entry ledger | `Account`, `LedgerService`, `WalletService` | ADR-001, ADR-003 | TQ-1, TQ-3, TQ-6 | `simulate-transfer` | [[double-entry-accounting-principles]] |
| **FR-2** | **Idempotent Transfers**: Require `Idempotency-Key` header; retries replay without double charge | `IdempotencyService`, `TransferController` | ADR-005 | TQ-1, TQ-3, TQ-4, NFR-SAFE-05 | `simulate-transfer` | [[api-idempotency-and-retry-semantics]] |
| **FR-3** | **Concurrent Balance Mutability**: Handle simultaneous transfers safely without races | `LedgerService` (`BEGIN IMMEDIATE`), WAL | ADR-002 | TQ-2, TQ-4, NFR-SAFE-01, 03 | `simulate-transfer` | [[sqlite-concurrency-and-acid-transactions]] |
| **FR-4** | **Immutable Audit Trail**: Append-only ledger; corrections via explicit reversals | `ledger_entry`, DB Triggers, `AdminService` | ADR-003 | TQ-3, NFR-SAFE-04, 06, DB-06 | `simulate-transfer` | [[immutable-ledger-and-audit-trails]] |
| **FR-5** | **Ledger Reconciliation**: Admin snapshot scan verifying credits = debits and balances | `ReconciliationService` (C1, C2, C3) | ADR-007 | TQ-3, NFR-SAFE-02, NFR-PERF-03 | `simulate-transfer` | [[financial-reconciliation-and-auditability]] |
| **FR-AUTH-01..06** | **Authentication & Roles**: USER, MERCHANT, ADMIN roles and JWT issuance | `AuthService`, `SecurityConfig` | ADR-006 | NFR-SEC-01..03, TQ-3 | `local-check` | [[api-security-and-input-validation]] |
| **FR-WAL-01..06** | **Wallet Management**: One wallet per owner/currency, test funding | `WalletService`, `AdminService` | ADR-001, ADR-003 | TQ-3, TQ-4, BR-06 | `simulate-transfer` | [[double-entry-accounting-principles]] |
| **FR-TRF-01..09** | **Fund Transfers**: Integer paise, bounds, balance checks, atomic debit/credit | `TransferService`, `LedgerService` | ADR-004, ADR-005 | TQ-1..04, TQ-6 | `simulate-transfer` | [[monetary-representation-and-integer-units]] |
| **FR-PAY-01..06** | **Merchant Payments**: User pays merchant code; merchants cannot transfer | `PaymentService`, `MerchantProfile` | ADR-001, ADR-006 | TQ-3, TQ-4 | `simulate-transfer` | [[api-security-and-input-validation]] |
| **FR-CON-01..05** | **Concurrency Control**: Serialized transactions, 503 busy handling | SQLite WAL, `BEGIN IMMEDIATE` | ADR-002 | TQ-2, TQ-4, NFR-PERF-01 | `simulate-transfer` | [[sqlite-concurrency-and-acid-transactions]] |
| **FR-LED-01..09** | **Ledger Invariants**: $\ge 2$ entries, debits = credits check, reversals | `LedgerService`, `ledger_entry` | ADR-003 | TQ-1, TQ-2, TQ-4 | `simulate-transfer` | [[double-entry-accounting-principles]] |
| **FR-REC-01..05** | **Reconciliation Logic**: C1 currency totals, C2 internal balance, C3 derivations | `ReconciliationService` | ADR-007 | TQ-3, NFR-SAFE-02 | `simulate-transfer` | [[financial-reconciliation-and-auditability]] |
| **FR-ADM-01..05** | **Administration**: Freeze/unfreeze, test funding, reversals, platform stats | `AdminService`, `WalletStatusChange` | ADR-001, ADR-003 | TQ-3, NFR-SEC-03 | `simulate-transfer` | [[immutable-ledger-and-audit-trails]] |
| **NFR-PERF-01..03** | **Performance**: $\le 1000$ ms latency, $\ge 10$ txn/s, recon $< 30$ s | SQLite WAL mode, Index optimization | ADR-002, ADR-007 | JMeter Load Tests | `local-check` | [[sqlite-concurrency-and-acid-transactions]] |
| **NFR-SAFE-01..06** | **Financial Safety**: ACID atomicity, consistency, isolation, durability | SQLite Engine, Triggers, Check constraints | ADR-002, ADR-003 | TQ-1, TQ-2, TQ-4 | `simulate-transfer` | [[sqlite-concurrency-and-acid-transactions]] |
| **NFR-SEC-01..06** | **Security**: BCrypt, JWT $\ge 256$ bit, parameterized SQL, input sanitization | `SecurityConfig`, `GlobalExceptionHandler` | ADR-006 | Security Scanning | `local-check` | [[api-security-and-input-validation]] |
| **DB-01..06** | **Database Rules**: SQLite file, WAL, integer columns, immutability triggers | `schema.sql`, `DatabaseConfig` | ADR-002, ADR-003 | TQ-5, TQ-6, NFR-SAFE-06 | `local-check` | [[sqlite-concurrency-and-acid-transactions]] |
| **ACC-01..04** | **Accounting Rules**: System funding asset, liability wallets, balanced entries | `LedgerService` | ADR-003 | TQ-1, TQ-2 | `simulate-transfer` | [[double-entry-accounting-principles]] |
| **TQ-1** | **Accounting Unit Tests**: Math, assertions, request hashing | `AccountingMathTest`, `HashTest` | ADR-004, ADR-005 | Unit Tests | `local-check` | [[monetary-representation-and-integer-units]] |
| **TQ-2** | **Transaction Rollback Integration Tests**: Simulated database failures | `RollbackIntegrationTest` | ADR-002 | Integration Tests | `local-check` | [[sqlite-concurrency-and-acid-transactions]] |
| **TQ-3** | **System Tests**: End-to-end API tests for FR-1 to FR-5 | `E2ETestSuite` | ADR-001 | End-to-End Tests | `simulate-transfer` | [[double-entry-accounting-principles]] |
| **TQ-4** | **Negative & Race Tests**: Negative balance attempts, simultaneous races | `ConcurrencyTest`, `NegativeTest` | ADR-002 | Concurrency Tests | `simulate-transfer` | [[sqlite-concurrency-and-acid-transactions]] |
| **TQ-5** | **Isolated CI Tests**: Run against fresh database instance in CI | GitHub Actions CI Pipeline | ADR-001 | CI Pipeline | `local-check` | [[sqlite-concurrency-and-acid-transactions]] |
| **TQ-6** | **Monetary Precision**: Exact integer paise without floating-point errors | `SkeletonSanityTest`, `MoneyUtils` | ADR-004 | Unit Tests | `local-check` | [[monetary-representation-and-integer-units]] |
| **SK-01** | **simulate-transfer**: Complete product workflow verification | Python / Script workflow | ADR-001..007 | System Verification | `simulate-transfer` | [[double-entry-accounting-principles]] |
| **SK-02** | **local-check**: Local developer build, test, and health verification | `brain/skills/local-check/SKILL.md` | ADR-001 | Local Environment | `local-check` | [[sqlite-concurrency-and-acid-transactions]] |
| **SK-03..05** | **Brain Skills**: Ingest, refine, query Second Brain vault | `brain/skills/` | ADR-001..007 | Second Brain Tests | `brain-query` | [[double-entry-accounting-principles]] |
