package com.quasar.ledger.module.wallet.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountStatus;
import com.quasar.ledger.module.wallet.model.AccountSummary;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistence port for ledger accounts (NFR-MAIN-03). */
public interface AccountRepository {

    void insert(Account account);

    /** Creates the System Funding Account for a currency if it does not exist yet (ACC-01). */
    void ensureSystemFunding(String currency, String createdAt);

    Optional<Account> findById(String id);

    List<Account> findByOwner(String ownerId);

    Optional<Account> findByOwnerAndCurrency(String ownerId, String currency);

    void updateStatus(String id, AccountStatus status);

    /**
     * Sets the cached balance. ONLY the Ledger module may call this (NFR-MAIN-02); the rule is
     * enforced by ArchitectureRulesTest.
     */
    void updateBalance(String id, long balanceMinor);

    List<AccountSummary> findAll(PageQuery page);

    long countAll();

    /** Wallet counts by status (FR-ADM-07). System Funding Accounts are not wallets and are excluded. */
    Map<AccountStatus, Long> countWalletsByStatus();

    /** Display name per account: user display name, merchant business name, or "System Funding". */
    Map<String, String> displayNames(Collection<String> accountIds);
}
