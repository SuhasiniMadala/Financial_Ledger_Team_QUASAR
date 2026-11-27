package com.quasar.ledger.module.transfer.dto;

/**
 * FR-LED-11/12: what a USER sees about a transaction. direction is OUTGOING or INCOMING from the caller's point of
 * view; counterpartyName is the other party's display name. Other parties' wallet balances are never included.
 */
public record UserTransactionView(String transactionId, String type, String status, String direction, String walletId,
                                  long amountMinor, String amountFormatted, String currency, String counterpartyName,
                                  String description, String failureCode, String reversalOfId, String reversedBy, String createdAt) {
}
