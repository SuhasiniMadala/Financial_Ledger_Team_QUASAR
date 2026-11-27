---
title: "Input Validation and Boundary Checking"
type: research
authors: [Akshaya A]
created: 2026-11-26
---

# Input Validation and Boundary Checking

## Summary

Input validation is the primary line of defense against malformed data, financial exploits, and system instability.

In financial applications, boundary checks guarantee that every numeric amount is an integer within defined ranges and prevent negative amounts from inflating wallet balances.

Strict syntactic and semantic validation prevents malformed requests from ever reaching the ledger domain.

## Core Validation Rules

1. **Strict Type Validation**: Ensure all amount parameters are integers (minor units) rather than floats or arbitrary strings.
2. **Positive Amount Enforcement**: Reject amounts $$\le 0$$ (zero or negative transfers) with HTTP 400.
3. **Upper Limit Constraints**: Reject single transaction amounts exceeding defined thresholds (e.g., ₹1,00,000.00 / 1,00,00,000 paise) with HTTP 400.
4. **Header Validation**: Validate `Idempotency-Key` format, length (8–64 characters), and permitted character set (alphanumeric, `-`, `_`).
5. **String Sanitization**: Enforce maximum length constraints on descriptions (140 chars) and references (64 chars) to prevent storage bloat and injection vulnerabilities.

## Relevance to Our Project

BR-01, BR-02, NFR-SEC-04, and Security Testing guidelines require comprehensive input validation.

Preventing negative values is critical to eliminate balance inflation attacks.

## Related Notes

- [[rest-api-security-and-testing]]
- [[integer-minor-units-and-monetary-precision]]
- [[idempotency-in-rest-apis]]

## Sources

- [OWASP – Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
- [RFC 9110 – HTTP Semantics: Status Codes and Validation](https://www.rfc-editor.org/rfc/rfc9110.html)
- [Project Statement: Financial Ledger & Digital Wallet API](../Financial_Wallet_Ledger_Project.pdf)
