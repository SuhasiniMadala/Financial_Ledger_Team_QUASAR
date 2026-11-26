# REST API Security and Testing

## Summary

REST API security involves protecting API endpoints, validating incoming requests and preventing unauthorized access or misuse.

Testing is necessary to verify that an API behaves correctly under normal, invalid and unexpected conditions.

Financial APIs require strong validation because incorrect or unauthorized requests can lead to financial losses.

## Common API Security Practices

### 1. Input Validation

Validate request parameters, wallet IDs, transaction amounts and other input values.

### 2. Authentication

Verify the identity of users before allowing access to protected endpoints.

### 3. Authorization

Ensure that users can access only the wallets and operations they are permitted to use.

### 4. Negative Amount Prevention

Reject negative transfer amounts to prevent invalid financial operations.

### 5. Error Handling

Return appropriate error responses without exposing sensitive internal information.

## Important Testing Types

### 1. Unit Testing

Test individual functions such as accounting calculations and idempotency key handling.

### 2. Integration Testing

Verify interactions between application components and the database.

### 3. System Testing

Test complete API workflows from request submission to final ledger updates.

### 4. Negative Testing

Verify that invalid requests are rejected correctly.

Examples:
- Negative transfer amounts.
- Insufficient wallet balance.
- Invalid idempotency keys.
- Malformed wallet IDs.

### 5. Concurrent Testing

Send multiple transfer requests simultaneously and verify that wallet balances remain correct.

## Relevance to Our Project

Our Financial Ledger API exposes REST endpoints for wallet creation, transfers and ledger reconciliation.

The project requires input validation, negative amount prevention, unit tests, integration tests, system tests and concurrency testing.

These practices help ensure that the API is reliable and secure.

## Related Notes

- [[idempotency-in-rest-apis]]
- [[database-concurrency-and-locking]]
- [[financial-ledger-reconciliation]]

## Sources

- [OWASP API Security Top 10](https://owasp.org/API-Security/)
- [OWASP API Security Project](https://owasp.org/www-project-api-security/)
- [Project Problem Statement](../Financial_Wallet_Ledger_Project.pdf)
