package com.quasar.ledger.module.auth.dto;

/**
 * Successful login (FR-AUTH-06). {@code displayName} is the user's display name, a merchant's business name or the
 * configured admin name; the React client shows it in the top bar (SRS 3.1). It is not put in the JWT, which keeps
 * only the account ID, role and expiry.
 */
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, String accountId, String role,
                            String displayName) {

    @Override
    public String toString() {
        return "LoginResponse[accountId=" + accountId + ", role=" + role + "]";
    }
}
