# Demo Guide - Member 3 (Suhasini M): Transfers, Merchant Payments, History and simulate-transfer

Environment: `docs/review/demo-environment.md`. Runs after Member 2's first part: Asha has ₹5,000.00, Ravi ₹2,000.00 (both INR, ACTIVE). Time: about 6 minutes plus the skill run.

Fill **Actual** and **Status** during the rehearsal and the live review. PASS only for what you saw yourself.

## Scenarios (SRS running example)

| # | Requirement | Role | Client screen / action | API call | Expected | Automated test | Actual | Status |
|---|---|---|---|---|---|---|---|---|
| 1 | FR-TRF-01, FR-TRF-14, UI-01/02/04 | USER Asha | Transfer: from Asha's INR wallet, to Ravi's wallet ID, amount `1,000.00`, description "rent share" -> Send money | `POST /transfers` + new `Idempotency-Key` | "Transfer completed", ₹1,000.00, `T-...` COMPLETED (201) | `TransferApiTest.successfulTransfer`, `UserPayments.test.tsx` | | NOT YET RUN |
| 2 | FR-TRF-14, FR-WAL-05 | USER Asha | "See my balances" | `GET /wallets` | ₹4,000.00 (Ravi: ₹3,000.00 when he logs in) - balances come from the backend | same | | NOT YET RUN |
| 3 | UI-05 | USER Asha | point at the button while sending ("Sending...", disabled) | - | one request only, even on double click | `UserPayments.test.tsx` "disables the button", `useFinancialSubmit.test.ts` | | NOT YET RUN |
| 4 | FR-TRF-05, UI-04 | USER Asha | **Idempotent replay, live:** stop the backend (terminal 1, Ctrl+C), submit a ₹10.00 transfer -> "Could not reach the server"; start the backend again; press **Retry (same request)** | `POST /transfers` twice with the **same key** | the transfer completes once; balance drops by ₹10.00 exactly once | `useFinancialSubmit.test.ts`, `TransferApiTest.replay`, `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` | | NOT YET RUN |
| 5 | FR-TRF-11, FR-TRF-13 | USER Asha | Transfer ₹6,000.00 | `POST /transfers` | "There is not enough money..." + link "Open failed transaction T-..." (422) | `TransferApiTest.insufficientFunds`, `UserPayments.test.tsx` | | NOT YET RUN |
| 6 | FR-TRF-09, UI-02 | USER Asha | amount `100.5` -> OK (₹100.50); amount `10.505` | none for `10.505` | "Use at most two decimal places (paise)." | `money.test.ts`, `TransferApiTest.invalidAmounts` | | NOT YET RUN |
| 7 | FR-PAY-01, FR-PAY-03 | USER Asha | Pay Merchant: Campus Cafe's code, ₹250.00, reference "Order 1187" | `POST /payments` + key | "Payment completed" (201) | `PaymentAndMerchantApiTest`, `UserPayments.test.tsx` | | NOT YET RUN |
| 8 | FR-PAY-02, BR-04 | USER Asha | Pay Merchant with code `MER-000000` | `POST /payments` | "The destination is not valid..." (422 INVALID_DESTINATION, FAILED recorded) | `PaymentAndMerchantApiTest.paymentRejections` | | NOT YET RUN |
| 9 | FR-LED-11 | USER Asha | History; filter Status = FAILED | `GET /transactions?status=FAILED` | the two failed attempts with failure codes; then all: newest first, + / - by direction | `TransactionHistoryApiTest`, `UserPayments.test.tsx` | | NOT YET RUN |
| 10 | FR-LED-12 | USER Asha | open the ₹1,000.00 transfer | `GET /transactions/{id}` | counterparty "Ravi Kumar", **only Asha's own entry** (DEBIT, balance after), no Ravi balance | `TransactionHistoryApiTest`, `UserPayments.test.tsx` | | NOT YET RUN |
| 11 | FR-PAY-04 | MERCHANT | log in as Campus Cafe -> Account | `GET /merchant/account` | business name, merchant code, wallet, ₹250.00, ACTIVE | `PaymentAndMerchantApiTest` | | NOT YET RUN |
| 12 | FR-PAY-05/06 | MERCHANT | Payments | `GET /merchant/transactions` | the payment with payer "Asha Rao" and "Order 1187"; **no payer wallet or balance** | `PaymentAndMerchantApiTest`, `Merchant.test.tsx` | | NOT YET RUN |
| 13 | FR-PAY-07 | MERCHANT | Statistics | `GET /merchant/statistics` | 1 payment, total ₹250.00, average ₹250.00 (last 30 days) | `MerchantAverageTest`, `Merchant.test.tsx` | | NOT YET RUN |
| 14 | BR-04, FR-AUTH-08 | MERCHANT | type http://localhost:5173/user/transfer | none | "Not allowed" (backend: 403 FORBIDDEN, SRS 4.4.2 #4) | `App.test.tsx`, `SecurityAccessMatrixTest` | | NOT YET RUN |

After Member 2 reverses the ₹1,000.00 transfer, show Asha's History again: the transfer shows "reversed by T-..." and a REVERSAL row credits her (FR-LED-07).

## simulate-transfer end-to-end (SK-01)

Run in a **third** terminal against the same backend (it creates its own synthetic `@simulate.test` users, so the demo data is not disturbed):

```powershell
$env:LEDGER_ADMIN_EMAIL = '<same as terminal 1>'
$env:LEDGER_ADMIN_PASSWORD = Read-Host 'Admin password used in terminal 1'
powershell -ExecutionPolicy Bypass -File brain/skills/simulate-transfer/scripts/simulate-transfer.ps1 -Evaluate -ReportPath target/simulate-transfer-report.md
```

Expected: steps 1-6 PASS, E01-E14 PASS, E15 NOT RUN (atomicity is covered by `TransferRollbackTest`), "Overall: PASS", exit code 0. Explain step 3 (same key twice = one transaction) and step 4 (10 simultaneous transfers worth more than the balance: some 422, never negative). Paste the report into `docs/testing/stage-4-skill-evaluation.md` (Run 3).

## Not shown live (and why)

| Requirement | Why | Evidence |
|---|---|---|
| FR-CON-06 (503 after the 5 s busy timeout) | needs an artificially held lock | `BusyTimeoutTest` |
| NFR-SAFE-03 (100 concurrent debits) | too slow to watch | `ConcurrencyApiTest.hundredConcurrentDebits`; simulate-transfer step 4 shows the same property live |
| NFR-PERF-01..03 | needs JMeter-scale load | `PerformanceBenchmarkTest`, results in `docs/testing/test-execution-report.md` |
