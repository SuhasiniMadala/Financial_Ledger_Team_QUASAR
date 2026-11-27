package com.quasar.ledger.module.wallet.model;

/** Row of the admin account list (FR-ADM-02): includes the owner's name and System Funding Accounts. */
public record AccountSummary(String accountId, String ownerId, String ownerName, AccountType type, String currency,
                             long balanceMinor, AccountStatus status, String createdAt) {
}
