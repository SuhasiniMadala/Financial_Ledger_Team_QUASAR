package com.quasar.ledger.module.admin.dto;

/** FR-ADM-01 row: ID, name, email, registration time (and merchant code for merchants). */
public record UserSummaryResponse(String accountId, String name, String email, String registeredAt, String merchantCode) {
}
