package com.quasar.ledger.module.ledger.dto;

import com.quasar.ledger.module.ledger.model.LedgerEntry;

/** FR-LED-04 fields of one ledger entry. */
public record LedgerEntryView(String entryId, long sequenceNo, String transactionId, String accountId, String direction,
                              long amountMinor, long balanceAfterMinor, String createdAt) {

    public static LedgerEntryView of(LedgerEntry e) {
        return new LedgerEntryView(e.id(), e.sequenceNo(), e.transactionId(), e.accountId(), e.direction().name(), e.amountMinor(),
                e.balanceAfterMinor(), e.createdAt());
    }
}
