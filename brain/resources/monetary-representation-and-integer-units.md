# Monetary Representation and Integer Units

## Summary

In software systems handling money, standard IEEE-754 floating-point representations (`float`, `double`) are strictly prohibited. Floating-point types represent numbers in binary base-2, where decimal fractions (such as 0.10 or 0.70) cannot be represented exactly, leading to cumulative rounding errors.

In double-entry financial systems, monetary amounts are strictly represented and calculated using **integer minor units** (paise for INR, cents for USD).

## Floating-Point Inaccuracies vs Integer Arithmetic

Consider adding ₹0.10 and ₹0.20 using double-precision floating-point numbers:
```
0.10 + 0.20 = 0.30000000000000004
```
In a ledger with millions of entries, floating-point drift creates phantom discrepancies, breaking reconciliation checks ($\sum \text{Debits} \ne \sum \text{Credits}$).

Using integer minor units:
```
10 paise + 20 paise = 30 paise (Exact integer math)
```

## Modeling Standards

1. **Database Columns**: Stored as 64-bit integers (`INTEGER` in SQLite, `Long` in Java).
   ```sql
   balance_minor INTEGER NOT NULL CHECK (balance_minor >= 0),
   amount_minor  INTEGER NOT NULL CHECK (amount_minor > 0)
   ```
2. **API Layer**: Transported in JSON payloads as integer `amountMinor` (e.g. ₹500.00 is transmitted as `50000`).
3. **UI Formatting**: Converted to decimal strings with currency symbols and regional grouping (₹500.00) strictly for presentation, and converted back via exact string parsing without intermediate floating-point types.

## Relevance to Project Requirements

- **TQ-6**: Money handled strictly using integer minor units or exact decimal types.
- **BR-01 & BR-02**: Amounts are positive integers representing minor units within transaction bounds (1 paisa to ₹1,00,000.00).
- **UI-01 & UI-02**: UI formatting using Indian grouping and exact string-to-paise parsing.

## Related Notes

- [[double-entry-accounting-principles]]
- [[api-security-and-input-validation]]
- [[financial-reconciliation-and-auditability]]

## Sources

- SQLite Documentation: *Floating Point Numbers* (https://www.sqlite.org/floatingpoint.html)
- Financial Ledger SRS v1.4, Section 1.2, Section 5.4 (TQ-6), Section 5.5 (BR-01)
