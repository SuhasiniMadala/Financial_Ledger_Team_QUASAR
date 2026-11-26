# ADR-003: Double-Entry Immutable Ledger as Single Source of Truth

## Status
Accepted

## Date
2026-11-26

## Context
Financial applications must treat money as an verifiable mathematical truth rather than an arbitrary editable number. In poorly designed architectures, balances are modified directly via SQL `UPDATE` statements without an audit trail, making fraud, lost funds, and accounting discrepancies impossible to trace.

We evaluated:
1. **Single-Entry Mutable Balance Table**: Accounts simply store a balance integer, updated directly with transactions logged optionally.
2. **Double-Entry Append-Only Ledger**: Balances are liabilities or assets; every money movement requires balanced debit and credit entries. Entries and transactions cannot be updated or deleted.

## Decision
We implement a **strict, tamper-evident, double-entry ledger** as the sole source of truth:
1. Every completed transaction posts at least two ledger entries such that:
   $$\sum \text{Debits} = \sum \text{Credits}$$
2. User and merchant wallets are liability accounts (Debit decreases balance, Credit increases balance).
3. The System Funding Account is an asset account (Debit increases balance, Credit decreases balance).
4. Direct modification (`UPDATE`) or deletion (`DELETE`) on transaction records, ledger entries, and wallet status changes is strictly prohibited and blocked by SQLite database triggers (`RAISE(ABORT)`).
5. In accordance with DB-05, ledger entries, transaction records, and wallet status change logs are retained indefinitely. Idempotency records are retained for a minimum of 24 hours.
6. Corrections or cancellations must be recorded exclusively as explicit `REVERSAL` transactions that post mirror-image entries and reference the original transaction ID.
7. Only the internal `Ledger` module is authorized to write ledger entries and update cached balances.

## Consequences
- **Positive**:
  - Mathematically guarantees that funds are neither created nor lost during transfers.
  - Complete, unalterable historical audit trail for forensic analysis and compliance.
  - Automated reconciliation can verify all recorded balances at any time.
- **Negative**:
  - Ledger storage requirements grow monotonically (append-only); requires index optimization on sequence numbers and account IDs.
  - Corrections require creating explicit reversal transactions rather than simply adjusting numbers.

## Citations
- Second Brain Reference: [[double-entry-accounting-principles]], [[immutable-ledger-and-audit-trails]], [[financial-reconciliation-and-auditability]]
- SRS Traceability: FR-1, FR-4, FR-LED-01 to FR-LED-09, ACC-01 to ACC-04, DB-03, DB-06, NFR-SAFE-06
