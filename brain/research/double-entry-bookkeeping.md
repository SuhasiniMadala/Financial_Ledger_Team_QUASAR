---
title: "Double-Entry Bookkeeping"
type: research
authors: [Geethika M]
created: 2026-11-25
---

# Double-Entry Bookkeeping

## Summary

Double-entry bookkeeping is an accounting methodology where every financial transaction is recorded using at least two entries: an equal and opposite debit and credit.

For every completed transaction, the total sum of debits must strictly equal the total sum of credits:
$$\sum \text{Debits} = \sum \text{Credits}$$

In a digital wallet system, user wallets are treated as liability accounts (money owed by the platform to users), while system funding accounts are treated as asset accounts.

This method maintains absolute accounting accuracy and makes it easy to identify inconsistencies or balance corruptions in financial records.

## How It Works

Consider a transfer of ₹500 from User A to User B:

- User A's wallet (Liability): Debit ₹500 (decreases balance)
- User B's wallet (Liability): Credit ₹500 (increases balance)

$$\text{Total Debit} = ₹500, \quad \text{Total Credit} = ₹500$$

The transaction remains strictly balanced.

### Core Accounting Mechanics

In our ledger model:
- **Wallet Accounts (Liabilities)**: A credit increases the wallet balance, while a debit decreases it.
- **System Funding Account (Asset)**: A debit increases the funding account, and a credit decreases it.
- **Initial Test Funding**: Debits System Funding (asset) and credits the user wallet (liability) equally.
- **Transfers & Payments**: Debits the payer wallet and credits the payee wallet equally.

## Error Detection & Auditability

Double-entry accounting guarantees that money is never created out of thin air or destroyed.

A discrepancy between total debits and credits immediately flags data corruption, concurrency anomalies, or software defects during reconciliation.

## Relevance to Our Project

Our Financial Ledger API uses double-entry bookkeeping to ensure that every transfer is recorded consistently.

Requirement FR-1 mandates that every wallet balance is backed by double-entry ledger entries.

Requirements ACC-01 through ACC-04 and FR-LED-01 to FR-LED-03 require balanced entries and check that total debits equal total credits before commit.

## Related Notes

- [[financial-ledger-reconciliation]]
- [[acid-transactions-and-sqlite]]
- [[acid-transactions]]
- [[immutable-audit-trails]]
- [[integer-minor-units-and-monetary-precision]]
- [[idempotency-in-rest-apis]]

## Sources

- [Project Problem Statement](../Financial_Wallet_Ledger_Project.pdf)
- [OpenStax – Principles of Financial Accounting](https://openstax.org/details/books/principles-financial-accounting)
- [Principles of Accounting – Double-Entry Bookkeeping](https://www.accountingcoach.com/accounting-basics/explanation/2)
- [Martin Fowler – Accounting Patterns](https://martinfowler.com/eaaDev/AccountingTransaction.html)
