package com.quasar.ledger.module.transfer.dto;

/** FR-PAY-04: business name, merchant code, wallet ID, balance and status. */
public record MerchantAccountResponse(String businessName, String merchantCode, String walletId, String currency,
                                      long balanceMinor, String balanceFormatted, String status) {
}
