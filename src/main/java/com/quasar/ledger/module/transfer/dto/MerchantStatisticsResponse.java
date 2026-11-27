package com.quasar.ledger.module.transfer.dto;

/** FR-PAY-07: count, total and average (rounded half-even to the nearest paisa) of COMPLETED payments in a date range. */
public record MerchantStatisticsResponse(String from, String to, String currency, long paymentCount, long totalMinor,
                                         String totalFormatted, long averageMinor, String averageFormatted) {
}
