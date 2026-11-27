package com.quasar.ledger.module.admin.dto;

import com.quasar.ledger.module.ledger.dto.LedgerEntryView;
import com.quasar.ledger.module.ledger.dto.TransactionView;
import java.util.List;

/** FR-ADM-03: an administrator sees a transaction with all of its ledger entries. */
public record TransactionDetailResponse(TransactionView transaction, List<LedgerEntryView> entries) {
}
