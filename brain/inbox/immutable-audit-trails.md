# Immutable Audit Trails

## Summary

In financial and double-entry bookkeeping systems, audit trails must be strictly append-only and tamper-evident.

Once a transaction or ledger entry is written and committed, it must never be modified or deleted.

Any correction, cancellation, or error adjustment must be recorded exclusively as an explicit new reversal transaction.

## Why Immutability is Essential

Traditional CRUD database design allows UPDATE and DELETE operations on existing records.

In financial accounting, allowing updates destroys traceability, hides errors or fraud, and creates discrepancies between historical reports and current state.

Treating financial records as an immutable event stream guarantees that historical financial truth is permanently verifiable.

## Enforcement Mechanisms

- **Database-Level Controls**: Disallow UPDATE and DELETE privileges on ledger and transaction tables; enforce SQLite triggers (`BEFORE UPDATE`, `BEFORE DELETE`) that raise `ABORT`.
- **API Design**: Expose only creation (POST) and retrieval (GET) endpoints for transactions. No PUT, PATCH, or DELETE operations are permitted on ledger records.
- **Explicit Reversals**: To reverse a transaction, an admin creates an inverse transaction that mirrors original accounts with inverted DEBIT and CREDIT entries, explicitly linking to the original transaction ID.

## Relevance to Our Project

Our requirement FR-4 mandates an Immutable Audit Trail where ledger records cannot be updated or deleted once written.

NFR-SAFE-06 and DB-06 require that direct SQL UPDATE or DELETE on ledger-entry and transaction tables fail.

Furthermore, BR-09 and ACC-04 specify how reversals are posted with balanced mirror entries.

## Related Notes

- [[double-entry-bookkeeping]]
- [[acid-transactions-and-sqlite]]
- [[financial-ledger-reconciliation]]
- [[database-concurrency-and-locking]]

## Sources

- [SQLite Triggers Documentation](https://www.sqlite.org/lang_createtrigger.html)
- [Martin Fowler – Accounting Patterns: Event Sourcing & Ledger Immutability](https://martinfowler.com/eaaDev/AccountingTransaction.html)
- [Project Problem Statement](../Financial_Wallet_Ledger_Project.pdf)
