package com.quasar.ledger.module.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, String accountId, String role) {

    @Override
    public String toString() {
        return "LoginResponse[accountId=" + accountId + ", role=" + role + "]";
    }
}
