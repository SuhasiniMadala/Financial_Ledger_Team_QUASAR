---
title: "Double-Entry Accounting Principles and Ledger Modeling"
type: resource
authors: [Akshaya A]
created: 2026-11-26
---

# Double-Entry Accounting Principles and Ledger Modeling

## Summary

Double-entry bookkeeping is the mathematical foundation of modern financial accounting. In a double-entry ledger, money is never created or destroyed arbitrarily; every economic event is modeled as a balanced transfer between at least two accounts.

For every completed transaction, the fundamental accounting equation holds:
$$\sum \text{Debits} = \sum \text{Credits}$$

## Account Types & Normal Balances

In our digital wallet architecture, all internal ledger accounts fall into one of two categories:

1. **Liability Accounts (User & Merchant Wallets)**:
   - Represent obligations owed by the platform to account holders.
   - **Credit (+)**: Increases wallet balance.
   - **Debit (-)**: Decreases wallet balance.
   - **Normal Balance**: Calculated as $\text{Credits} - \text{Debits} \ge 0$.
2. **Asset Accounts (System Funding Accounts)**:
   - Represent platform-managed source funds for simulated testing.
   - **Debit (+)**: Increases funding balance.
   - **Credit (-)**: Decreases funding balance.
   - **Normal Balance**: Calculated as $\text{Debits} - \text{Credits}$.

## Global Ledger Conservation Invariant

Across the entire platform, for each currency $C$:
$$\sum \text{System Funding Account Balances}_C = \sum \text{Wallet Balances}_C$$

This invariant guarantees that the total liabilities held in user wallets are mathematically backed by system asset accounts.

## Transaction Entry Patterns

| Transaction Type | Debit Account | Credit Account | Description |
|---|---|---|---|
| `TEST_FUNDING` | System Funding (Asset) | User Wallet (Liability) | Admin credits simulated funds to an active user wallet |
| `TRANSFER` | Payer Wallet (Liability) | Payee Wallet (Liability) | Peer-to-peer fund transfer between active user wallets |
| `MERCHANT_PAYMENT` | Customer Wallet (Liability) | Merchant Wallet (Liability) | Consumer payment to a registered merchant code |
| `REVERSAL` | Original Credit Account | Original Debit Account | Mirror-image cancellation linked to original transaction |

## Relevance to Project Requirements

- **FR-1**: Every wallet balance is strictly backed by double-entry ledger records.
- **ACC-01 to ACC-04**: Define system funding accounts, balance derivations, and posting rules.
- **FR-LED-01 & FR-LED-02**: Enforce $\ge 2$ entries per transaction and debit = credit verification prior to commit.

## Related Notes

- [[financial-reconciliation-and-auditability]]
- [[sqlite-concurrency-and-acid-transactions]]
- [[immutable-ledger-and-audit-trails]]
- [[monetary-representation-and-integer-units]]

## Sources

- Martin Fowler, *Accounting Patterns: Event Sourcing & Ledger Immutability*
- OpenStax, *Principles of Financial Accounting*
- Financial Ledger SRS v1.4, Section 6.2
