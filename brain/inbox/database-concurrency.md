# Database Concurrency

## Summary

Database concurrency refers to the ability of a database system to handle multiple transactions at the same time.

In financial applications, multiple users may perform transfers or balance updates on the same wallet simultaneously.

Without proper concurrency control, transactions may interfere with each other and cause incorrect balances or lost updates.

## Why Is Concurrency Important?

Consider a wallet with ₹1000.

Two transactions occur simultaneously:

- Transaction A attempts to debit ₹300.
- Transaction B attempts to debit ₹500.

Both transactions must be handled safely without overwriting each other's updates or allowing an invalid balance.

## Common Concurrency Problems

### 1. Lost Update

Two transactions read the same balance and independently update it. One update may overwrite the other.

### 2. Dirty Read

A transaction reads data modified by another transaction that has not yet been committed.

### 3. Non-Repeatable Read

A transaction reads the same record twice but gets different values because another transaction modified it.

## Concurrency Control Techniques

- Database transactions
- Locking mechanisms
- Isolation levels
- Atomic updates

## Relevance to Our Project

Our FR-3 requirement specifies safe handling of simultaneous incoming and outgoing transfers on the same wallet.

Concurrency control is essential to prevent balance corruption and ensure that all transfers are processed consistently.

## Related Notes

- [[ACID Transactions]]
- [[SQLite Transactions and WAL Mode]]
- [[Database Locking and Race Conditions]]
- [[Idempotency in REST APIs]]

## Sources

- [PostgreSQL – Concurrency Control](https://www.postgresql.org/docs/current/mvcc.html)
- [SQLite – Isolation](https://www.sqlite.org/isolation.html)