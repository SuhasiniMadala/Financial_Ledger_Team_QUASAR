package com.quasar.ledger.module.auth.model;

/** Business details of a MERCHANT account (FR-AUTH-02). */
public record MerchantProfile(String id, String userAccountId, String businessName, String merchantCode) {
}
