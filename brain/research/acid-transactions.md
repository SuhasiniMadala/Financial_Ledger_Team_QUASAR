---
title: "ACID Transactions"
type: research
authors: [Geethika M]
created: 2026-11-25
---

# ACID Transactions

## Summary

ACID stands for Atomicity, Consistency, Isolation and Durability.

These four properties ensure that database transactions are processed reliably.

They are especially important in financial applications where incomplete or inconsistent transactions can cause incorrect balances.

## Four ACID Properties

### 1. Atomicity

A transaction must either complete entirely or have no effect.

Example: If a wallet transfer fails halfway, the debit must be rolled back.

### 2. Consistency

A transaction must preserve database integrity rules.

Example: Every completed transfer must maintain equal debit and credit entries.

### 3. Isolation

Concurrent transactions should not interfere with one another in a way that produces incorrect results.

Example: Two simultaneous transfers from the same wallet must not corrupt its balance.

### 4. Durability

Once a transaction is committed, its changes must persist even after a system failure.

## Relevance to Our Project

Our Financial Ledger API depends on ACID transactions to maintain accurate wallet balances.

Atomicity and consistency help ensure that transfers are recorded correctly, while isolation helps handle concurrent requests safely.

Durability ensures that successfully committed transactions are preserved.

## Related Notes

- [[double-entry-bookkeeping]]
- [[database-concurrency]]
- [[sqlite-transactions-and-wal-mode]]

## Sources

- [SQLite Transaction Documentation](https://www.sqlite.org/lang_transaction.html)
- [Project Problem Statement](../Financial_Wallet_Ledger_Project.pdf)
