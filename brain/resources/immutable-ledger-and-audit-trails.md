# Immutable Ledger and Audit Trails

## Summary

In financial accounting, auditability requires an immutable, tamper-evident log of events. Traditional CRUD systems allow destructive `UPDATE` and `DELETE` operations on existing database rows. In a double-entry financial ledger, committed transactions and entries are strictly append-only.

Any error, mistaken transfer, or cancellation must be recorded as a new, explicit `REVERSAL` transaction that cancels the effects of the original transaction while preserving the complete historical record.

## Mechanics of Append-Only Immutability

1. **Write Once, Read Many (WORM)**:
   - Once inserted with status `COMPLETED` or `FAILED`, a transaction row is never modified.
   - Ledger entries are strictly immutable.
2. **Database-Level Trigger Enforcement**:
   - SQLite triggers enforce immutability at the storage engine level, preventing accidental or malicious SQL modifications even if application code is bypassed:
     ```sql
     CREATE TRIGGER prevent_ledger_entry_update
     BEFORE UPDATE ON ledger_entry
     BEGIN
         SELECT RAISE(ABORT, 'Ledger entries are immutable and cannot be updated');
     END;

     CREATE TRIGGER prevent_ledger_entry_delete
     BEFORE DELETE ON ledger_entry
     BEGIN
         SELECT RAISE(ABORT, 'Ledger entries are immutable and cannot be deleted');
     END;

     CREATE TRIGGER prevent_status_change_update
     BEFORE UPDATE ON wallet_status_change
     BEGIN
         SELECT RAISE(ABORT, 'Wallet status change audit records are immutable and cannot be updated');
     END;

     CREATE TRIGGER prevent_status_change_delete
     BEFORE DELETE ON wallet_status_change
     BEGIN
         SELECT RAISE(ABORT, 'Wallet status change audit records cannot be deleted');
     END;
     ```
3. **Data Retention Requirements (DB-05)**:
   - **Indefinite Retention**: Committed `ledger_entry`, `transaction_record`, and `wallet_status_change` records must be retained indefinitely (`DB-05`, `FR-4`).
   - **Idempotency Retention**: `idempotency_record` entries must be kept for at least 24 hours (`FR-TRF-04`) to safely detect retries, after which expired keys may be deleted.
4. **Explicit Reversal Mechanics**:
   - To reverse transaction $T_{\text{orig}}$ of amount $A$:
     - A new transaction $T_{\text{rev}}$ of type `REVERSAL` is created with attribute `reversal_of_id = T_{\text{orig}}.id`.
     - Opposite ledger entries are posted: if $T_{\text{orig}}$ debited Account A and credited Account B, $T_{\text{rev}}$ debits Account B and credits Account A by amount $A$.
     - Transactions can be reversed at most once.

## Relevance to Project Requirements

- **FR-4**: Immutable Audit Trail preventing update or deletion of committed records.
- **DB-06**: Database triggers blocking `UPDATE` and `DELETE` operations on ledger tables.
- **NFR-SAFE-06**: Immutability safety requirement verified by automated direct SQL modification tests.
- **BR-09 & BR-10**: Rules governing reversal transactions.

## Related Notes

- [[double-entry-accounting-principles]]
- [[sqlite-concurrency-and-acid-transactions]]
- [[financial-reconciliation-and-auditability]]

## Sources

- Martin Fowler, *Event Sourcing and Accounting Transactions*
- SQLite Trigger Documentation (https://www.sqlite.org/lang_createtrigger.html)
- Financial Ledger SRS v1.4, Section 4.6 & Section 6.1
