package com.quasar.ledger.module.auth.model;

import com.quasar.ledger.common.security.Role;

/** A registered USER, MERCHANT or configured ADMIN. The password is only ever held as a BCrypt hash. */
public record UserAccount(String id, String email, String passwordHash, String displayName, Role role, String createdAt) {

    @Override
    public String toString() {
        // Never print the password hash (NFR-SEC-06).
        return "UserAccount[id=" + id + ", role=" + role + "]";
    }
}
