
# Double-Entry Bookkeeping

## Summary

Double-entry bookkeeping is an accounting method where every financial transaction is recorded using at least two entries: a debit and a credit.

The total debit amount must always equal the total credit amount.

This method helps maintain accounting accuracy and makes it easier to identify inconsistencies in financial records.

## How It Works

Consider a transfer of ₹500 from User A to User B.

- User A's wallet: Debit ₹500
- User B's wallet: Credit ₹500

Total Debit = ₹500
Total Credit = ₹500

The transaction remains balanced.

## Relevance to Our Project

Our Financial Ledger API uses double-entry bookkeeping to ensure that every transfer is recorded consistently.

Every credit must have an equal and opposite debit. This helps prevent incorrect financial records and supports ledger reconciliation.

## Related Notes

- [[Financial Ledger Reconciliation]]
- [[ACID Transactions]]
- [[Idempotency in REST APIs]]

## Sources

- [Project Problem Statement](../Financial_Wallet_Ledger_Project.pdf)
- [OpenStax – Principles of Financial Accounting](https://openstax.org/details/books/principles-financial-accounting)
