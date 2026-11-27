package com.quasar.ledger.module.auth.dto;

/** Returned with HTTP 201 after registration. merchantCode and walletId are only set for merchants. */
public record RegistrationResponse(String accountId, String role, String email, String displayName, String createdAt,
                                   String merchantCode, String walletId) {
}
