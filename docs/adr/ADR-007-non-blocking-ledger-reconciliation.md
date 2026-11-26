# ADR-007: Non-Blocking Point-in-Time Ledger Reconciliation

## Status
Accepted

## Date
2026-11-26

## Context
Requirement FR-5 mandates an administrative reconciliation endpoint that scans ledger entries, verifies that total credits equal total debits, and confirms that recorded wallet balances match ledger sums.

If reconciliation locks the entire database or attempts to write fixes to discrepancies, it interferes with active transaction processing, creates lock contention, or masks serious underlying software bugs.

We evaluated:
1. **Destructive Auto-Repair Reconciliation**: Automatically updating wallet balances when mismatches are detected.
2. **Non-Destructive Snapshot Audit**: Running checks in a consistent read-only snapshot, reporting discrepancies without modifying data.

## Decision
We implement **non-destructive, read-only reconciliation executed within a single SQLite read transaction**:
1. Triggered via `POST /api/v1/admin/reconciliations`.
2. Operates entirely inside a read snapshot under SQLite WAL mode, allowing concurrent financial write transactions to proceed without blocking or generating false alarms.
3. Executes three core verification checks:
   - **C1**: Total debits equal total credits per currency.
   - **C2**: Every completed transaction contains balanced entries ($\ge 2$ entries, $\text{Debits} = \text{Credits}$).
   - **C3**: Every account's stored balance equals its cumulative ledger balance.
4. **Zero State Mutation**: Reconciliation never creates, updates, or deletes financial data.
5. Returns a structured JSON report detailing run time, counts checked, overall status (`PASS` or `FAIL`), and any detected discrepancy.

## Consequences
- **Positive**:
  - Purely audit-driven; preserves historical truth and never corrupts data with flawed automated repairs.
  - WAL mode snapshot guarantees non-blocking concurrency during active platform use.
  - Satisfies NFR-PERF-03 (completes over 100,000 entries in $< 30$ seconds).
- **Negative**:
  - Discrepancies require administrative investigation and explicit reversal transactions rather than one-click automatic fixes.

## Citations
- Second Brain Reference: [[financial-reconciliation-and-auditability]], [[sqlite-concurrency-and-acid-transactions]]
- SRS Traceability: FR-5, FR-REC-01 to FR-REC-05, NFR-SAFE-02, NFR-PERF-03
