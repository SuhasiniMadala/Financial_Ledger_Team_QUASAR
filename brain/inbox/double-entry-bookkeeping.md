# Double-Entry Bookkeeping Principles

## Summary

Double-entry bookkeeping is an accounting methodology where every financial transaction is recorded with equal and opposite entries into at least two accounts.

For every transaction, the total sum of debits must strictly equal the total sum of credits ($$\sum \text{Debits} = \sum \text{Credits}$$).

In a digital wallet system, user wallets are treated as liability accounts, while system funding accounts are treated as asset accounts.

## Core Accounting Mechanics

In our ledger model:
- **Wallet Accounts (Liabilities)**: A credit increases the wallet balance (money owed to the user), while a debit decreases the wallet balance.
- **System Funding Account (Asset)**: A debit increases the funding account, and a credit decreases it.
- **Initial Test Funding**: Debits System Funding and credits the user wallet equally.
- **Transfers & Payments**: Debits the payer wallet and credits the payee wallet equally.

## Error Detection & Auditability

Double-entry accounting prevents funds from disappearing or appearing without an originating source.

A discrepancy between total debits and credits immediately flags data corruption or software defects during reconciliation.

## Relevance to Our Project

Requirement FR-1 mandates that every wallet balance is backed by double-entry ledger entries.

ACC-01 through ACC-04 and FR-LED-01 to FR-LED-03 require balanced entries and check that debits equal credits before commit.

## Related Notes

- [[financial-ledger-reconciliation]]
- [[acid-transactions-and-sqlite]]
- [[immutable-audit-trails]]
- [[integer-minor-units-and-monetary-precision]]

## Sources

- [Principles of Accounting – Double-Entry Bookkeeping](https://www.accountingcoach.com/accounting-basics/explanation/2)
- [Martin Fowler – Accounting Patterns](https://martinfowler.com/eaaDev/AccountingTransaction.html)
- [Project Statement: Financial Ledger & Digital Wallet API](../Financial_Wallet_Ledger_Project.pdf)
