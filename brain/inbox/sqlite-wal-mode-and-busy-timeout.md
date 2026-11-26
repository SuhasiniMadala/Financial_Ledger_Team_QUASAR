# SQLite WAL Mode and Busy Timeout

## Summary

Write-Ahead Logging (WAL) is a SQLite journaling mode that significantly improves database concurrency and read/write performance.

Under traditional rollback journals, readers block writers and writers block readers.

Under WAL mode, readers do not block writers, and a writer does not block readers.

## How WAL Mode Operates

1. **Write-Ahead Log**: Changes are appended to a separate `.db-wal` file rather than written directly to the main database file.
2. **Concurrent Reads**: Readers read a consistent snapshot from the database file and the WAL index without waiting for the writer.
3. **Single Writer**: SQLite still permits only one active writer at a time.
4. **Busy Timeout**: If a writer encounters a locked database, `busy_timeout` dictates how long SQLite will retry before returning `SQLITE_BUSY`.
5. **Checkpoints**: Committed WAL pages are periodically transferred back to the main database file.

## Configuration Best Practices

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = FULL;
PRAGMA busy_timeout = 5000;
PRAGMA foreign_keys = ON;
```

## Relevance to Our Project

SRS DB-02 mandates configuring SQLite with WAL mode, foreign keys enabled, `synchronous=FULL`, and a 5000 ms busy timeout.

NFR-PERF-01 and FR-CON-03 require clean timeout handling returning HTTP 503 `SERVICE_BUSY` with `Retry-After`.

## Related Notes

- [[acid-transactions-and-sqlite]]
- [[database-concurrency-and-locking]]
- [[immutable-audit-trails]]

## Sources

- [SQLite – Write-Ahead Logging](https://www.sqlite.org/wal.html)
- [SQLite – PRAGMA Statements](https://www.sqlite.org/pragma.html)
- [Project Statement: Financial Ledger & Digital Wallet API](../Financial_Wallet_Ledger_Project.pdf)
