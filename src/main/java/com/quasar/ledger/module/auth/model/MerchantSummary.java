package com.quasar.ledger.module.auth.model;

/** Row of the admin merchant list (FR-ADM-01). */
public record MerchantSummary(String accountId, String businessName, String email, String merchantCode, String registeredAt) {
}
