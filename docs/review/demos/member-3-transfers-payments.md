# Demo Guide - Member 3 (Suhasini M): Transfers, Merchant Payments, History and simulate-transfer

Environment: `docs/review/demo-environment.md`. Runs after Member 2's first part: Asha has ₹5,000.00, Ravi ₹2,000.00 (both INR, ACTIVE). Time: about 6 minutes plus the skill run.

Fill **Actual** and **Status** during the rehearsal and the live review. PASS only for what you saw yourself.

## Scenarios (SRS running example)

| # | Requirement | Role | Client screen / action | API call | Expected | Automated test | Actual | Status |
|---|---|---|---|---|---|---|---|---|
| 1 | FR-TRF-01, FR-TRF-14, UI-01/02/04 | USER Asha | Transfer: from Asha's INR wallet, to Ravi's wallet ID, amount `1,000.00`, description "rent share" -> Send money | `POST /transfers` + new `Idempotency-Key` | "Transfer completed", ₹1,000.00, `T-...` COMPLETED (201) | `TransferApiTest.successfulTransfer`, `UserPayments.test.tsx` | Rehearsal 2026-11-28: "Transfer completed", ₹1,000.00 sent from W-Q5SBA2RR2P to W-Y3KG3MAVJH, T-04HEEQJZRW0F COMPLETED | PASS |
| 2 | FR-TRF-14, FR-WAL-05 | USER Asha | "See my balances" | `GET /wallets` | ₹4,000.00 (Ravi: ₹3,000.00 when he logs in) - balances come from the backend | same | Rehearsal 2026-11-28: Asha's wallet W-Q5SBA2RR2P shows ₹4,000.00 (400000 paise), ACTIVE (Ravi's view not opened in this step) | PASS |
| 3 | UI-05 | USER Asha | point at the button while sending ("Sending...", disabled) | - | one request only, even on double click | `UserPayments.test.tsx` "disables the button", `useFinancialSubmit.test.ts` | Rehearsal 2026-11-28: ₹1.00 transfer "button grey"; the button greyed out while sending; Asha's history shows exactly one such transfer (T-3FVX6TC4J1P6), balance ₹3,999.00 | PASS |
| 4 | FR-TRF-05, UI-04 | USER Asha | **Idempotent replay, live:** stop the backend (terminal 1, Ctrl+C), submit a ₹10.00 transfer -> "Could not reach the server"; start the backend again; press **Retry (same request)** | `POST /transfers` twice with the **same key** | the transfer completes once; balance drops by ₹10.00 exactly once | `useFinancialSubmit.test.ts`, `TransferApiTest.replay`, `ConcurrencyApiTest.sameKeyConcurrentlyExecutesOnce` | Rehearsal 2026-11-28: backend stopped; ₹10.00 "retry test" -> "Could not reach the server..." with **Retry (same request)**; backend restarted (same database); Retry -> "Transfer completed", T-TMVQP26YVES0. Asha has exactly one "retry test" transfer; balance ₹3,999.00 -> ₹3,989.00 (charged once) | PASS |
| 5 | FR-TRF-11, FR-TRF-13 | USER Asha | Transfer ₹6,000.00 | `POST /transfers` | "There is not enough money..." + link "Open failed transaction T-..." (422) | `TransferApiTest.insufficientFunds`, `UserPayments.test.tsx` | Rehearsal 2026-11-28: amount entered ₹60,000.00 (not ₹6,000.00; both exceed the ₹3,989.00 balance) -> "There is not enough money in the wallet for this amount. Recorded as failed transaction T-N14FNJKNDXMX." + link "Open failed transaction T-N14FNJKNDXMX" | PASS |
| 6 | FR-TRF-09, UI-02 | USER Asha | amount `100.5` -> OK (₹100.50); amount `10.505` | none for `10.505` | "Use at most two decimal places (paise)." | `money.test.ts`, `TransferApiTest.invalidAmounts` | Rehearsal 2026-11-28: `100.5` -> "Transfer completed", ₹100.50, T-CE0BRYZY9BRC (balance ₹3,989.00 -> ₹3,888.50); `10.505` -> "Use at most two decimal places (paise)." shown in the form, nothing sent (balance stayed ₹3,888.50) | PASS |
| 7 | FR-PAY-01, FR-PAY-03 | USER Asha | Pay Merchant: Campus Cafe's code, ₹250.00, reference "Order 1187" | `POST /payments` + key | "Payment completed" (201) | `PaymentAndMerchantApiTest`, `UserPayments.test.tsx` | Rehearsal 2026-11-28: Campus Cafe MER-11R75X, ₹250.00, reference "Order 1187" -> "Payment completed", ₹250.00 sent from W-Q5SBA2RR2P to W-AT3KYM9VSE, T-H5KN9B21TA5X COMPLETED (balance ₹3,888.50 -> ₹3,638.50) | PASS |
| 8 | FR-PAY-02, BR-04 | USER Asha | Pay Merchant with code `MER-000000` | `POST /payments` | "The destination is not valid..." (422 INVALID_DESTINATION, FAILED recorded) | `PaymentAndMerchantApiTest.paymentRejections` | Rehearsal 2026-11-28: code entered `MER-11R788` (not `MER-000000`; also a code no merchant has), ₹50 -> "The destination is not valid. Users send to user wallets and pay merchants by merchant code. Recorded as failed transaction T-2DX085KK3DZ5." + link to the failed transaction | PASS |
| 9 | FR-LED-11 | USER Asha | History; filter Status = FAILED | `GET /transactions?status=FAILED` | the two failed attempts with failure codes; then all: newest first, + / - by direction | `TransactionHistoryApiTest`, `UserPayments.test.tsx` | Rehearsal 2026-11-28: Status = FAILED -> exactly the 2 failed attempts, "Page 1 of 1 (2 total)": T-2DX085KK3DZ5 MERCHANT_PAYMENT - ₹50.00 and T-N14FNJKNDXMX TRANSFER (Ravi Kumar) - ₹60,000.00, failure codes in the Notes column (partly off-screen in the screenshot). Status = All -> newest first (11:01:51, 11:01:22, 10:59:45 UTC ...), outgoing amounts shown with "-" | PASS |
| 10 | FR-LED-12 | USER Asha | open the ₹1,000.00 transfer | `GET /transactions/{id}` | counterparty "Ravi Kumar", **only Asha's own entry** (DEBIT, balance after), no Ravi balance | `TransactionHistoryApiTest`, `UserPayments.test.tsx` | Rehearsal 2026-11-28: T-04HEEQJZRW0F TRANSFER COMPLETED, Money out, ₹1,000.00, counterparty Ravi Kumar, "rent share"; My ledger entries: only W-Q5SBA2RR2P DEBIT ₹1,000.00, my balance after ₹4,000.00; no Ravi wallet entry or balance shown | PASS |
| 11 | FR-PAY-04 | MERCHANT | log in as Campus Cafe -> Account | `GET /merchant/account` | business name, merchant code, wallet, ₹250.00, ACTIVE | `PaymentAndMerchantApiTest` | Rehearsal 2026-11-28: Account shows "Campus Cafe", merchant code MER-11R75X, wallet W-AT3KYM9VSE (INR), balance ₹250.00, status ACTIVE | PASS |
| 12 | FR-PAY-05/06 | MERCHANT | Payments | `GET /merchant/transactions` | the payment with payer "Asha Rao" and "Order 1187"; **no payer wallet or balance** | `PaymentAndMerchantApiTest`, `Merchant.test.tsx` | Rehearsal 2026-11-28: "Payments received", 1 total: 2026-11-28 11:01:22 UTC, T-H5KN9B21TA5X, Payment, payer Asha Rao, reference "Order 1187", + ₹250.00; columns are only time, transaction, type, payer, reference, amount (no payer wallet or balance) | PASS |
| 13 | FR-PAY-07 | MERCHANT | Statistics | `GET /merchant/statistics` | 1 payment, total ₹250.00, average ₹250.00 (last 30 days) | `MerchantAverageTest`, `Merchant.test.tsx` | Rehearsal 2026-11-28: default range 2026-10-30T00:00Z to 2026-11-29T00:00Z (UTC, inclusive): completed payments 1, total ₹250.00, average ₹250.00 | PASS |
| 14 | BR-04, FR-AUTH-08 | MERCHANT | type http://localhost:5173/user/transfer | none | "Not allowed" (backend: 403 FORBIDDEN, SRS 4.4.2 #4) | `App.test.tsx`, `SecurityAccessMatrixTest` | Rehearsal 2026-11-28: logged in as Campus Cafe, typed localhost:5173/user/transfer -> "Not allowed", "Your role cannot open this screen." + "Back to my home page" (no transfer form shown) | PASS |

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
