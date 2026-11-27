package com.quasar.ledger.module.wallet.model;

/**
 * A ledger account: a user wallet, a merchant wallet or a per-currency System Funding Account.
 * {@code balanceMinor} is a cached value that only the Ledger module may change (NFR-MAIN-02);
 * reconciliation proves it equals the balance derived from ledger entries (FR-REC-04).
 */
public record Account(String id, String ownerId, AccountType type, String currency, long balanceMinor,
                      AccountStatus status, String createdAt) {

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isOwnedBy(String accountId) {
        return ownerId != null && ownerId.equals(accountId);
    }
}
