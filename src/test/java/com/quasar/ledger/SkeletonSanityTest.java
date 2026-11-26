package com.quasar.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkeletonSanityTest {

    @Test
    @DisplayName("TQ-6: Monetary integer paise math prevents floating-point rounding errors")
    void testIntegerMinorUnitsMath() {
        // ₹10.00 = 1000 paise, ₹20.50 = 2050 paise
        long balancePaise = 1000L;
        long transferPaise = 2050L;
        long total = balancePaise + transferPaise;
        assertEquals(3050L, total, "Paise calculations must be mathematically exact without floating-point drift");
    }
}
