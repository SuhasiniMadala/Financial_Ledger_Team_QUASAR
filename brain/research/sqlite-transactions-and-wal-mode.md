---
title: "SQLite Transactions and WAL Mode"
type: research
authors: [Suhasini M]
created: 2026-11-25
---

# SQLite Transactions and WAL Mode

## Summary

SQLite is a lightweight, serverless relational database that stores data in a database file.

It supports transactions that allow multiple database operations to be executed as a single logical unit.

SQLite also provides Write-Ahead Logging (WAL) mode, which changes how database changes are recorded and can improve concurrency between readers and writers.

## SQLite Transactions

A transaction generally follows three important operations:

### 1. BEGIN

Starts a database transaction.

### 2. COMMIT

Permanently saves the changes made during the transaction.

### 3. ROLLBACK

Reverts the changes if an operation fails before the transaction is committed.

## What Is WAL Mode?

In Write-Ahead Logging mode, database changes are first written to a separate WAL file rather than immediately modifying the main database file.

This allows readers to continue reading the database while a writer is writing.

However, SQLite still permits only one writer at a time.

## Example

During a wallet transfer:

1. Begin a transaction.
2. Debit the sender's wallet.
3. Credit the receiver's wallet.
4. Insert the corresponding ledger entries.
5. Commit the transaction.

If any operation fails, rollback the transaction.

## Relevance to Our Project

Our project encourages SQLite because it provides built-in transaction support without requiring a separate database server.

Transactions help maintain financial consistency, while WAL mode can improve reader-writer concurrency.

## Related Notes

- [[acid-transactions]]
- [[database-concurrency]]
- [[database-locking-and-race-conditions]]
- [[double-entry-bookkeeping]]

## Sources

- [SQLite – Transactions](https://www.sqlite.org/lang_transaction.html)
- [SQLite – Write-Ahead Logging](https://www.sqlite.org/wal.html)
- [SQLite – Isolation](https://www.sqlite.org/isolation.html)