package com.quasar.ledger.module.wallet.dto;

import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.module.wallet.model.Account;

/** FR-WAL-06: wallet ID, currency, balance in minor units and formatted, and status. */
public record WalletResponse(String walletId, String currency, long balanceMinor, String balanceFormatted, String status,
                             String createdAt) {

    public static WalletResponse from(Account a) {
        return new WalletResponse(a.id(), a.currency(), a.balanceMinor(), MoneyFormatter.format(a.balanceMinor(), a.currency()),
                a.status().name(), a.createdAt());
    }
}
