package com.quasar.ledger.common.security;

/** The caller identity taken from a verified JWT (account ID and role, FR-AUTH-06). */
public record AuthenticatedUser(String accountId, Role role) {
}
