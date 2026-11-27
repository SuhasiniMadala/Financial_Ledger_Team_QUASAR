package com.quasar.ledger.module.transfer.dto;

/**
 * FR-PAY-05/06: a payment or reversal on the merchant's wallet with time, amount, payer display name and reference.
 * Deliberately has no field for the payer's wallet ID or balance.
 */
public record MerchantTransactionView(String transactionId, String type, String direction, long amountMinor, String amountFormatted,
                                      String currency, String payerName, String reference, String reversalOfId, String createdAt) {
}
