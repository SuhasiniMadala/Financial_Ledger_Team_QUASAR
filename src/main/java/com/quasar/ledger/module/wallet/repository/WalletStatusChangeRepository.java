package com.quasar.ledger.module.wallet.repository;

import com.quasar.ledger.module.wallet.model.WalletStatusChange;
import java.util.List;

/** Persistence port for the append-only wallet status audit log (FR-ADM-05). */
public interface WalletStatusChangeRepository {

    void insert(WalletStatusChange change);

    List<WalletStatusChange> findByAccount(String accountId);
}
