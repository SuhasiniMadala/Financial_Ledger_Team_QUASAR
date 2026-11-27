package com.quasar.ledger.module.transfer.dto;

import com.quasar.ledger.module.ledger.dto.LedgerEntryView;
import java.util.List;

/** FR-LED-12: transaction details plus ONLY the caller's own ledger entries. */
public record UserTransactionDetail(UserTransactionView transaction, List<LedgerEntryView> ownEntries) {
}
