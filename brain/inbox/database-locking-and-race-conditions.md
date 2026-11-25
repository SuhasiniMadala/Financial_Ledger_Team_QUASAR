# Database Locking and Race Conditions

## Summary

A race condition occurs when multiple operations access and modify shared data concurrently, and the final result depends on the order in which they execute.

Database locking helps control access to shared records and prevents conflicting operations from causing inconsistent data.

This is particularly important in systems that handle financial transactions.

## What Is a Race Condition?

Consider a wallet with ₹1000.

Two requests simultaneously read the balance:

- Request A reads ₹1000 and wants to debit ₹300.
- Request B reads ₹1000 and wants to debit ₹500.

If both requests independently calculate and write the new balance, one update may overwrite the other.

The final balance could become incorrect.

## How Locking Helps

Database locking coordinates concurrent access to shared data.

A transaction may acquire the required lock before modifying data and release it after committing or rolling back.

This helps prevent conflicting updates.

## Common Locking Concepts

### 1. Read Lock

Allows reading data while restricting conflicting modifications, depending on the database locking model.

### 2. Write Lock

Controls modifications to shared data and prevents conflicting writers.

### 3. Transaction Locking

Locks are managed as part of database transaction processing to maintain consistency.

## Relevance to Our Project

Our FR-3 requirement focuses on safely handling simultaneous transfers involving the same wallet.

We need to prevent race conditions that could produce incorrect balances or allow invalid transactions.

SQLite transactions and appropriate write-locking strategies can help achieve this.

## Related Notes

- [[Database Concurrency]]
- [[SQLite Transactions and WAL Mode]]
- [[ACID Transactions]]
- [[Double-Entry Bookkeeping]]

## Sources

- [SQLite – Locking and Concurrency](https://www.sqlite.org/lockingv3.html)
- [SQLite – Transactions](https://www.sqlite.org/lang_transaction.html)