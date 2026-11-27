package com.quasar.ledger.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** UI-01 / FR-WAL-06 / TQ-6 / NFR-TEST-06: formatting uses integer arithmetic only. */
class MoneyFormatterTest {

    @ParameterizedTest(name = "{0} paise -> {1}")
    @CsvSource(delimiter = '|', value = {
        "0|₹0.00",
        "1|₹0.01",
        "99|₹0.99",
        "100|₹1.00",
        "100000|₹1,000.00",
        "500000|₹5,000.00",
        "925000|₹9,250.00",
        "10000000|₹1,00,000.00",
        "1234567891|₹1,23,45,678.91"
    })
    void formatsRupeesWithIndianGrouping(long paise, String expected) {
        assertThat(MoneyFormatter.format(paise, "INR")).isEqualTo(expected);
    }

    @Test
    void formatsDollarsWithWesternGrouping() {
        assertThat(MoneyFormatter.format(123456789L, "USD")).isEqualTo("$1,234,567.89");
        assertThat(MoneyFormatter.format(5L, "USD")).isEqualTo("$0.05");
    }

    @Test
    @DisplayName("NFR-TEST-06: 10 paise + 20 paise is exactly 30 paise (no floating-point drift)")
    void integerMinorUnitsAreExact() {
        long sum = 10L + 20L;
        assertThat(sum).isEqualTo(30L);
        assertThat(MoneyFormatter.format(sum, "INR")).isEqualTo("₹0.30");
        // The same calculation in binary floating point is NOT exact - the reason money is never a double (ADR-004).
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);
    }
}
