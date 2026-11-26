# Financial Reconciliation and Auditability

## Summary

Reconciliation is an independent verification process that assesses the mathematical consistency and integrity of financial ledger entries against recorded account balances.

Reconciliation operates without altering any financial records: it scans the ledger, computes cumulative balances, checks system-wide invariants, and reports any discrepancy found.

## The Three Reconciliation Checks

An automated reconciliation run performs three distinct mathematical checks:

### Check 1: System-Wide Currency Equality (C1)
Across all ledger entries in a given currency:
$$\sum_{\text{all entries}} \text{Amount}_{\text{Debit}} = \sum_{\text{all entries}} \text{Amount}_{\text{Credit}}$$
If total debits do not equal total credits, the ledger has suffered uncontained corruption.

### Check 2: Transaction Internal Balance (C2)
Every transaction $T$ with status `COMPLETED` must satisfy:
1. Entry count $\ge 2$.
2. $\sum_{e \in T} \text{Debit}_e = \sum_{e \in T} \text{Credit}_e$.
Transactions with status `FAILED` must have exactly zero ledger entries.

### Check 3: Stored Balance vs Ledger Sum Verification (C3)
For each account $A$:
- If $A$ is a **Wallet** (Liability Account):
  $$\text{Derived Balance}_A = \sum_{e \in \text{Entries}(A)} \text{Credit}_e - \sum_{e \in \text{Entries}(A)} \text{Debit}_e$$
- If $A$ is **System Funding** (Asset Account):
  $$\text{Derived Balance}_A = \sum_{e \in \text{Entries}(A)} \text{Debit}_e - \sum_{e \in \text{Entries}(A)} \text{Credit}_e$$
- Check: $\text{Stored Balance}_A = \text{Derived Balance}_A$.

## Non-Blocking Snapshot Execution

In high-concurrency systems, running reconciliation while writes occur can generate false-positive discrepancies if reads span multiple database states.

Under SQLite WAL mode, reconciliation executes inside a **single read transaction** (`BEGIN ... COMMIT` or read snapshot). It reads a consistent point-in-time snapshot, allowing concurrent transfers to proceed without blocking and without causing phantom reconciliation failures.

## Relevance to Project Requirements

- **FR-5**: Administrative ledger reconciliation endpoint (`POST /admin/reconciliations`).
- **FR-REC-01 to FR-REC-05**: Invariant definitions, snapshot execution, and discrepancy reporting.
- **NFR-PERF-03**: Reconciliation over 100,000 entries finishes within 30 seconds.

## Related Notes

- [[double-entry-accounting-principles]]
- [[sqlite-concurrency-and-acid-transactions]]
- [[immutable-ledger-and-audit-trails]]

## Sources

- Financial Ledger SRS v1.4, Section 4.7
- *Auditing and Assurance Services*, Arens et al.
