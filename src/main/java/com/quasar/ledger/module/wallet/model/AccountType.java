package com.quasar.ledger.module.wallet.model;

/** Wallets are liabilities (credit raises the balance); SYSTEM_FUNDING is an asset (debit raises it). See SRS 6.2. */
public enum AccountType {
    USER_WALLET,
    MERCHANT_WALLET,
    SYSTEM_FUNDING;

    /** ACC-02: true when a DEBIT increases this account's balance. */
    public boolean isDebitNormal() {
        return this == SYSTEM_FUNDING;
    }
}
