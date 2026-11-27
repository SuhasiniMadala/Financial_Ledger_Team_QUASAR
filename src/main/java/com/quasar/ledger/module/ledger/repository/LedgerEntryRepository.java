package com.quasar.ledger.module.ledger.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.module.ledger.model.LedgerEntry;
import java.util.List;

/** Persistence port for ledger entries. Insert-only (FR-LED-05). Only LedgerService may call insert (NFR-MAIN-02). */
public interface LedgerEntryRepository {

    void insert(LedgerEntry entry);

    List<LedgerEntry> findByTransaction(String transactionId);

    List<LedgerEntry> findByAccount(String accountId, PageQuery page);

    long countByAccount(String accountId);
}
