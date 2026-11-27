package com.quasar.ledger.module.wallet.model;

/** Immutable audit record of an admin status change (FR-ADM-05). */
public record WalletStatusChange(String id, String accountId, AccountStatus oldStatus, AccountStatus newStatus,
                                 String changedBy, String reason, String createdAt) {
}
