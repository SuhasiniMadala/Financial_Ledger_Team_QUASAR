# ADR-008: Supported Currencies (TBD-1) and Per-Transaction Limit (TBD-2)

## Status
Accepted

## Date
2026-11-27

## Context
The SRS left two items open and asked for them to be decided and recorded as an ADR (Appendix C):
- **TBD-1** - which currencies besides the mandatory INR are supported ("candidate: USD, to demonstrate distinct-currency wallets").
- **TBD-2** - the final per-transaction limit (default Rs 1,00,000.00).

Several requirements cannot be tested without these answers: FR-WAL-01/03 (create a wallet "in any supported currency", reject unsupported ones), BR-03 / FR-TRF-12 (`CURRENCY_MISMATCH`), SRS 4.4.2 #2 ("pays from a USD wallet"), and BR-02 / FR-TRF-09 (limit).

## Decision
1. **Currencies are configuration**: `ledger.currencies=INR,USD`. INR is mandatory (start-up fails without it). Codes must be 3 upper-case letters. One System Funding Account per configured currency is created at start-up (ACC-01): `SYS-FUND-INR`, `SYS-FUND-USD`.
2. **No conversion** between currencies (BR-03). Merchants receive INR only (FR-WAL-02), so paying from a USD wallet is `422 CURRENCY_MISMATCH`.
3. **Limit is configuration** in minor units: `ledger.max-amount-minor=10000000` (Rs 1,00,000.00). The same numeric limit applies to every currency (for USD that is $100,000.00). Amounts above it are `400 VALIDATION_ERROR` and nothing is recorded (FR-TRF-09).
4. All money stays an integer number of minor units, as in ADR-004 and [[monetary-representation-and-integer-units]].

## Consequences
- Positive: every currency rule in the SRS is now testable and tested (`WalletApiTest`, `TransferApiTest.otherBusinessRules`, `PaymentAndMerchantApiTest.paymentRejections`, `TestFundingApiTest.amountBoundaries`).
- Positive: changing the limit after the instructor confirms TBD-2 is a configuration change, not a code change.
- Negative: one numeric limit for all currencies is a simplification; a per-currency limit would need a map in configuration.
- The limit should still be confirmed with the instructor (SRS TBD-2 says "confirm with instructor").

## Citations
- [[monetary-representation-and-integer-units]]
- [[double-entry-accounting-principles]] (one System Funding asset account per currency)
- SRS: TBD-1, TBD-2, BR-02, BR-03, FR-WAL-01..03, FR-TRF-09, ACC-01
