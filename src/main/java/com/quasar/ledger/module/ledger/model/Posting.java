package com.quasar.ledger.module.ledger.model;

/** An instruction to post one entry; the Ledger turns a balanced list of postings into entries. */
public record Posting(String accountId, EntryDirection direction, long amountMinor) {

    public static Posting debit(String accountId, long amountMinor) {
        return new Posting(accountId, EntryDirection.DEBIT, amountMinor);
    }

    public static Posting credit(String accountId, long amountMinor) {
        return new Posting(accountId, EntryDirection.CREDIT, amountMinor);
    }
}
