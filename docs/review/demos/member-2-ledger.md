# Demo Guide - Member 2 (Geethika M): Ledger, Test Funding, Reversals and Reconciliation

Environment: `docs/review/demo-environment.md`. Runs after Member 1's demo (Asha and Ravi have INR wallets at ₹0.00; Ravi's wallet is ACTIVE). Time: about 6 minutes.

Fill **Actual** and **Status** during the rehearsal and the live review. PASS only for what you saw yourself.

## Scenarios

| # | Requirement | Role | Client screen / action | API call | Expected | Automated test | Actual | Status |
|---|---|---|---|---|---|---|---|---|
| 1 | FR-WAL-07, ACC-03 | ADMIN | Wallets -> Asha's wallet -> **Test funding** -> 5000 -> Add simulated funds | `POST /admin/wallets/{id}/test-funding` + `Idempotency-Key` | "Test funding recorded", ₹5,000.00, a `T-...` link (201) | `TestFundingApiTest`, `AdminLedger.test.tsx` | | NOT YET RUN |
| 2 | FR-WAL-07 | ADMIN | Test funding for Ravi: 2000 | same | ₹2,000.00 recorded | same | | NOT YET RUN |
| 3 | UI-02 | ADMIN | Test funding with `10.505` | none (client check) | "Use at most two decimal places (paise)." Nothing sent | `money.test.ts`, `AdminLedger.test.tsx` | | NOT YET RUN |
| 4 | FR-LED-01/02, ACC-03, golden rule | ADMIN | open the funding's `T-...` link | `GET /admin/transactions/{id}` | 2 entries: `SYS-FUND-INR` DEBIT ₹5,000.00 and Asha CREDIT ₹5,000.00; "Debits = credits: balanced" | `LedgerServiceTest`, `TestFundingApiTest.goldenRule` | | NOT YET RUN |
| 5 | FR-ADM-03, FR-LED-04 | ADMIN | click Asha's account in the entries table | `GET /admin/accounts/{id}/entries` | entry with sequence no., direction, amount, balance after | `AdminLedgerQueryApiTest` | | NOT YET RUN |
| 6 | FR-ADM-03 | ADMIN | Transactions & Ledger, filter Type = TEST_FUNDING | `GET /admin/transactions?type=TEST_FUNDING` | the two fundings only | `AdminLedgerQueryApiTest` | | NOT YET RUN |
| -  | | | **Member 3 demo runs here** (transfer T-501 Asha -> Ravi ₹1,000.00, payment, failed attempt) | | | | | |
| 7 | FR-LED-07, ACC-04 | ADMIN | Transactions & Ledger -> open Asha's ₹1,000.00 TRANSFER -> reason "sent to wrong recipient" -> Reverse | `POST /admin/transactions/{id}/reversal` + key | "Reversed by T-..."; the new REVERSAL has mirror-image entries (DEBIT Ravi, CREDIT Asha); the original is unchanged but shows "Reversed by" | `ReversalApiTest`, `AdminLedger.test.tsx` | | NOT YET RUN |
| 8 | FR-LED-09, BR-10 | ADMIN | open the same transfer again | `GET /admin/transactions/{id}` | no Reverse form any more (reversed once only). Swagger alternative: second reversal -> 409 ALREADY_REVERSED | `ReversalApiTest` | | NOT YET RUN |
| 9 | FR-LED-09 | ADMIN | open the REVERSAL or a FAILED transaction | same | no Reverse form (422 NOT_REVERSIBLE if forced through the API) | `ReversalApiTest` | | NOT YET RUN |
| 10 | FR-LED-08 | ADMIN | reason "short" on another completed transaction | none (client check) | "The reason must be 10 to 500 characters." | `AdminLedger.test.tsx` | | NOT YET RUN |
| 11 | FR-REC-01..08, NFR-SAFE-02 | ADMIN | Reconciliation -> Run reconciliation | `POST /admin/reconciliations` | **PASS**; INR total debits = total credits; counts of accounts/transactions/entries; 0 discrepancies | `ReconciliationApiTest` | | NOT YET RUN |
| 12 | FR-ADM-07 | ADMIN | Statistics | `GET /admin/statistics` | users, merchants, wallets by status, COMPLETED/FAILED counts, totals by type, latest reconciliation PASS | `AdminLedgerQueryApiTest.statistics`, `AdminLedger.test.tsx` | | NOT YET RUN |

## Not shown live (and why)

| Requirement | Why not live | Evidence instead |
|---|---|---|
| FR-LED-05, NFR-SAFE-06 (database rejects UPDATE/DELETE) | needs direct SQL on the file | `LedgerImmutabilityTest`; optionally `sqlite3` on a copy of the demo file: `UPDATE ledger_entry SET amount_minor = 1` fails with the trigger message |
| SRS 4.7.2 #2 (reconciliation FAIL after a manual balance edit) | corrupts the demo database | `ReconciliationDetectsCorruptionTest` (BALANCE_MISMATCH with expected, actual, difference) |
| FR-CON-07 / NFR-SAFE-01 (rollback on failure) | needs fault injection | `RollbackFailureInjectionTest` (3 failure points), `TransferRollbackTest` |

## What to say

- Money only enters through test funding: System Funding (an asset) is debited and the wallet (a liability) credited, so System Funding balance = sum of all wallets ([[double-entry-accounting-principles]]).
- Nothing is ever edited: a mistake is fixed by a new REVERSAL with mirror-image entries ([[immutable-ledger-and-audit-trails]], ADR-003).
- Reconciliation is read-only and runs on one snapshot, so transfers committing meanwhile cannot cause false alarms (ADR-007, [[financial-reconciliation-and-auditability]]).
