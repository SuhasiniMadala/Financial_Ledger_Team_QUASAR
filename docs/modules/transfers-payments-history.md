# Module: Transfers, Payments, Merchant Views and History (Member 3 - Suhasini M)

Issues: #13, #14, #15, #16, #17. Requirements: FR-TRF-01..14, FR-PAY-01..07, FR-LED-11/12, FR-CON-04..08, BR-03..07, NFR-SAFE-03..05, NFR-PERF-01..03, SK-01.

## Components

| Class | Role |
|---|---|
| `MoneyMovementController` | `POST /api/v1/transfers`, `POST /api/v1/payments` (USER, Idempotency-Key required) |
| `TransferService` | source must be the caller's USER wallet (else 404, nothing recorded); destination must be a USER wallet |
| `PaymentService` | destination is the merchant's wallet found by merchant code |
| `TransferRules` | shared rule order: INVALID_DESTINATION, SAME_WALLET, CURRENCY_MISMATCH, WALLET_NOT_ACTIVE, INSUFFICIENT_FUNDS - each recorded as a FAILED transaction and a stored 422 |
| `TransactionHistoryService` / `TransactionHistoryController` | `GET /api/v1/transactions(/{id})`: COMPLETED transactions on own wallets (incoming and outgoing) + own FAILED attempts; detail shows only own entries |
| `MerchantService` / `MerchantController` | `GET /api/v1/merchant/account`, `/transactions` (payer name + reference, never payer wallet or balance), `/statistics` (half-even average, default 30 days; a reversed payment stays COMPLETED and is still counted - the refund is the linked REVERSAL, see `PaymentAndMerchantApiTest.statisticsCountReversedPaymentsAsCompleted`) |

## Example: SRS 4.3.2 scenario 5

`POST /api/v1/transfers` Rs 6,000 from a Rs 5,000 wallet with key `K` -> `422 {"code":"INSUFFICIENT_FUNDS", ..., "transactionId":"T-..."}`; a FAILED transaction with no entries exists; the key is stored; resending with `K` returns the same 422 with `Idempotent-Replayed: true`.

## Tests

`TransferApiTest` (11), `TransferRollbackTest` (1), `PaymentAndMerchantApiTest` (6), `MerchantAverageTest` (8), `TransactionHistoryApiTest` (4), `ConcurrencyApiTest` (4), `BusyTimeoutTest` (1), `DurabilityTest` (1), `OpenApiDocumentationTest` (1), `SkillDefinitionsTest` (3), `PerformanceBenchmarkTest` (manual, tag `perf`).

Skill: `simulate-transfer`. Knowledge: [[testing-concurrency-and-failure-injection]], [[api-idempotency-and-retry-semantics]].
