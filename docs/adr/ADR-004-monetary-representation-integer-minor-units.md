# ADR-004: Monetary Representation in Integer Minor Units (Paise)

## Status
Accepted

## Date
2026-11-26

## Context
IEEE-754 binary floating-point types (`float`, `double`) cannot accurately represent decimal fractions like 0.10 or 0.70. Repeated financial operations using floating-point types accumulate rounding errors, leading to "fractional cent/paisa leaks" and reconciliation failures.

We evaluated:
1. **Floating-Point (`Double` / `Float`)**: Built-in CPU primitives.
2. **Arbitrary-Precision Decimals (`BigDecimal`)**: Exact decimal math in Java.
3. **64-bit Integer Minor Units (`Long` / `INTEGER`)**: Representing money as integer paise (1 Rupee = 100 paise).

While `BigDecimal` avoids rounding errors, storing decimals in databases introduces precision parsing overhead, floating-point ambiguity in SQLite numeric affinity, and serialization complexity.

## Decision
We mandate that **all monetary amounts be stored, calculated, and transmitted strictly as integer minor units (paise)**:
1. Database columns for balances and transfer amounts use SQLite `INTEGER` (64-bit integer) with check constraints (`amount_minor > 0`, `balance_minor >= 0`).
2. Java entities, domain services, and DTOs use `long` / `Long`.
3. REST API JSON payloads transport amounts as integer `amountMinor` (e.g. ₹100.50 is transported as `10050`).
4. Conversion between Rupees and paise occurs strictly at user interface boundaries using exact string parsing and string formatting without intermediate floating-point types.

## Consequences
- **Positive**:
  - Zero floating-point drift or precision loss.
  - Highly efficient integer comparison and arithmetic.
  - Native compatibility with SQLite integer math and constraints.
- **Negative**:
  - Requires explicit currency-scale handling if currencies with non-2 decimal precision are added in the future (currently INR with scale 2).

## Citations
- Second Brain Reference: [[monetary-representation-and-integer-units]]
- SRS Traceability: TQ-6, BR-01, DB-03, UI-01, UI-02
