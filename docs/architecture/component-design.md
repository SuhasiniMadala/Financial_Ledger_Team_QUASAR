# Component Design and Class Models

## 1. Package Structure

The Spring Boot backend is organized by feature and architectural layer:

```
com.quasar.ledger
├── FinancialLedgerApplication.java
├── common
│   ├── api
│   │   ├── ApiResponse.java
│   │   ├── ErrorResponse.java
│   │   └── ErrorCode.java
│   ├── exception
│   │   ├── ApiException.java
│   │   ├── InsufficientFundsException.java
│   │   ├── IdempotencyException.java
│   │   ├── ServiceBusyException.java
│   │   └── GlobalExceptionHandler.java
│   └── util
│       ├── MoneyUtils.java            // Exact string <-> paise conversions
│       └── HashUtils.java             // SHA-256 fingerprint generator
├── config
│   ├── DatabaseConfig.java            // SQLite WAL mode, foreign keys, busy timeout
│   ├── SecurityConfig.java            // JWT filter chain, role-based endpoint mapping
│   └── OpenApiConfig.java             // Swagger / OpenAPI documentation
├── module
│   ├── auth
│   │   ├── controller.AuthController
│   │   ├── model.UserAccount
│   │   ├── model.MerchantProfile
│   │   ├── model.Role (USER, MERCHANT, ADMIN)
│   │   ├── repository.AccountRepository
│   │   └── service.AuthService
│   ├── wallet
│   │   ├── controller.WalletController
│   │   ├── model.Account (USER_WALLET, MERCHANT_WALLET, SYSTEM_FUNDING)
│   │   ├── model.AccountStatus (ACTIVE, FROZEN, DEACTIVATED)
│   │   ├── repository.WalletRepository
│   │   └── service.WalletService
│   ├── transfer
│   │   ├── controller.TransferController
│   │   ├── controller.PaymentController
│   │   ├── dto.TransferRequest
│   │   ├── dto.PaymentRequest
│   │   └── service.TransferService
│   ├── idempotency
│   │   ├── model.IdempotencyRecord
│   │   ├── repository.IdempotencyRepository
│   │   └── service.IdempotencyService
│   ├── ledger
│   │   ├── model.TransactionRecord
│   │   ├── model.TransactionType (TEST_FUNDING, TRANSFER, MERCHANT_PAYMENT, REVERSAL)
│   │   ├── model.TransactionStatus (COMPLETED, FAILED)
│   │   ├── model.LedgerEntry
│   │   ├── model.EntryDirection (DEBIT, CREDIT)
│   │   ├── repository.TransactionRepository
│   │   ├── repository.LedgerEntryRepository
│   │   └── service.LedgerService      // Sole writer interface
│   ├── reconciliation
│   │   ├── controller.ReconciliationController
│   │   ├── dto.ReconciliationReport
│   │   └── service.ReconciliationService
│   └── admin
│       ├── controller.AdminController
│       └── service.AdminService
```

## 2. Core Domain Class Diagram

```mermaid
classDiagram
    class UserAccount {
        +String id
        +String email
        +String passwordHash
        +String displayName
        +Role role
        +Instant createdAt
    }

    class MerchantProfile {
        +String id
        +String userAccountId
        +String businessName
        +String merchantCode
    }

    class Account {
        +String id
        +String ownerId
        +AccountType accountType
        +String currency
        +long balanceMinor
        +AccountStatus status
        +boolean isActive()
    }

    class TransactionRecord {
        +String id
        +TransactionType type
        +TransactionStatus status
        +long amountMinor
        +String currency
        +String initiatedBy
        +String reversalOfId
        +String failureCode
        +Instant createdAt
    }

    class LedgerEntry {
        +String id
        +long sequenceNo
        +String transactionId
        +String accountId
        +EntryDirection direction
        +long amountMinor
        +long balanceAfterMinor
        +Instant createdAt
    }

    class IdempotencyRecord {
        +String principalId
        +String idemKey
        +String requestHash
        +int responseStatus
        +String responseBody
        +String transactionId
        +Instant expiresAt
    }

    class Role {
        <<enumeration>>
        USER
        MERCHANT
        ADMIN
    }

    class AccountType {
        <<enumeration>>
        USER_WALLET
        MERCHANT_WALLET
        SYSTEM_FUNDING
    }

    class AccountStatus {
        <<enumeration>>
        ACTIVE
        FROZEN
        DEACTIVATED
    }

    class TransactionType {
        <<enumeration>>
        TEST_FUNDING
        TRANSFER
        MERCHANT_PAYMENT
        REVERSAL
    }

    class EntryDirection {
        <<enumeration>>
        DEBIT
        CREDIT
    }

    UserAccount "1" -- "0..1" MerchantProfile : has
    UserAccount "1" -- "0..*" Account : owns (wallets)
    Account "1" -- "0..*" LedgerEntry : posted to
    TransactionRecord "1" -- "2..*" LedgerEntry : contains
    TransactionRecord "0..1" -- "0..1" TransactionRecord : reverses
    UserAccount "1" -- "0..*" IdempotencyRecord : submits
    TransactionRecord "0..1" -- "0..1" IdempotencyRecord : recorded for
```

## 3. Service Layer Collaboration Diagram

```mermaid
classDiagram
    class TransferService {
        +transferFunds(TransferRequest, principalId, idemKey)
    }

    class IdempotencyService {
        +findReplay(principalId, idemKey, requestHash)
        +storeOutcome(principalId, idemKey, status, body, txnId)
    }

    class WalletService {
        +createWallet(ownerId, currency, requestedType) Account
        +validateWalletRoleMatch(ownerId, requestedType) void
        +getWallet(walletId, principalId) Account
        +updateStatus(walletId, newStatus, reason, adminId) void
    }

    class LedgerService {
        <<interface>>
        +postTransaction(TransactionType, amount, currency, entries) TransactionRecord
        +assertTransactionBalanced(List~LedgerEntry~ entries) void
        +executeReversal(originalTxnId, reason, adminId, idemKey)
        +executeFunding(walletId, amount, adminId, idemKey)
    }

    class ReconciliationService {
        +runReconciliationAudit()
    }

    TransferService --> IdempotencyService : coordinates with
    TransferService --> LedgerService : delegates financial mutation
    WalletService --> Account : manages
    ReconciliationService ..> LedgerService : verifies invariants against
```
