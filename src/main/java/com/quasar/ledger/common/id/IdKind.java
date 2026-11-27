package com.quasar.ledger.common.id;

/** The kinds of identifiers the system generates, with their prefix and random-part length. */
public enum IdKind {
    USER("U-", 10),
    MERCHANT_PROFILE("M-", 10),
    MERCHANT_CODE("MER-", 6),
    WALLET("W-", 10),
    TRANSACTION("T-", 12),
    LEDGER_ENTRY("E-", 12),
    STATUS_CHANGE("S-", 10),
    RECONCILIATION_RUN("R-", 10);

    private final String prefix;
    private final int length;

    IdKind(String prefix, int length) {
        this.prefix = prefix;
        this.length = length;
    }

    public String prefix() {
        return prefix;
    }

    public int length() {
        return length;
    }
}
