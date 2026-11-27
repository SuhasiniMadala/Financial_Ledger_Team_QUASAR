package com.quasar.ledger.module.transfer.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** FR-PAY-07 / TQ-6: the average is rounded half-even to a whole paisa using exact decimal arithmetic. */
class MerchantAverageTest {

    @ParameterizedTest(name = "total {0} / count {1} = {2}")
    @CsvSource({
        "0, 0, 0",
        "25000, 1, 25000",
        "301, 3, 100",
        "3, 2, 2",
        "5, 2, 2",
        "7, 2, 4",
        "1, 3, 0",
        "2, 3, 1"
    })
    void averageIsHalfEven(long total, long count, long expected) {
        assertThat(MerchantService.averageHalfEven(total, count)).isEqualTo(expected);
    }
}
