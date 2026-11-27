package com.quasar.ledger.module.wallet.model;

/** Wallet status (BR-07, FR-ADM-04): ACTIVE and FROZEN are reversible; DEACTIVATED is final. */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    DEACTIVATED
}
