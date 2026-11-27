package com.quasar.ledger.module.auth.repository;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.security.Role;
import com.quasar.ledger.module.auth.model.UserAccount;
import java.util.List;
import java.util.Optional;

/** Persistence port for accounts (NFR-MAIN-03). */
public interface UserAccountRepository {

    void insert(UserAccount account);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(String id);

    List<UserAccount> findByRole(Role role, PageQuery page);

    long countByRole(Role role);
}
