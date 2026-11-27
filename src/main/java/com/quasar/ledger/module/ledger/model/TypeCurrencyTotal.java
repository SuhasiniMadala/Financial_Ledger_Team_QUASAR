package com.quasar.ledger.module.ledger.model;

/** Count and total of COMPLETED transactions per type and currency (FR-ADM-07). */
public record TypeCurrencyTotal(TransactionType type, String currency, long count, long totalMinor) {
}
