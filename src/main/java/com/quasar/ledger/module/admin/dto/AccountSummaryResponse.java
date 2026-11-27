package com.quasar.ledger.module.admin.dto;

import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.module.wallet.model.AccountSummary;

/** FR-ADM-02 row: owner, type, currency, balance and status of every account, System Funding included. */
public record AccountSummaryResponse(String accountId, String ownerId, String ownerName, String type, String currency,
                                     long balanceMinor, String balanceFormatted, String status, String createdAt) {

    public static AccountSummaryResponse from(AccountSummary a) {
        return new AccountSummaryResponse(a.accountId(), a.ownerId(), a.ownerName(), a.type().name(), a.currency(),
                a.balanceMinor(), MoneyFormatter.format(a.balanceMinor(), a.currency()), a.status().name(), a.createdAt());
    }
}
