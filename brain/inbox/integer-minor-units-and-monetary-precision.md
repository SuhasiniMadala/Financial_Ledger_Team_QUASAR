# Integer Minor Units and Monetary Precision

## Summary

Financial applications must represent monetary values accurately.

Floating-point numbers can introduce rounding errors because many decimal fractions cannot be represented exactly in binary floating-point format.

A common solution is to store money as integer minor units, such as paise instead of rupees.

## Why Avoid Floating-Point Numbers?

Consider an application that repeatedly performs financial calculations using floating-point values.

Small representation and rounding errors can accumulate and produce inaccurate financial results.

Such inaccuracies are unacceptable in financial systems.

## Integer Minor Units

Instead of storing ₹100.50 as a floating-point number, the application can store it as:

₹100.50 = 10050 paise

Similarly:

₹25.75 = 2575 paise

Calculations can then be performed using integers for currencies with two decimal places.

## Advantages

- Avoids common binary floating-point representation errors.
- Provides predictable monetary calculations.
- Makes balance comparisons easier.
- Helps maintain accurate financial records.

## Important Consideration

Different currencies can have different minor-unit scales.

The system must use the appropriate currency precision rather than assuming every currency has two decimal places.

## Relevance to Our Project

Our TQ-6 requirement specifies that monetary values must be handled using integer minor units or exact decimal types.

This prevents floating-point rounding errors and helps ensure accurate wallet balances and ledger entries.

## Related Notes

- [[double-entry-bookkeeping]]
- [[financial-ledger-reconciliation]]
- [[acid-transactions-and-sqlite]]

## Sources

- [SQLite – Floating Point Numbers](https://www.sqlite.org/floatingpoint.html)
- [Python – Decimal Arithmetic](https://docs.python.org/3/library/decimal.html)
- [Project Problem Statement](../Financial_Wallet_Ledger_Project.pdf)
