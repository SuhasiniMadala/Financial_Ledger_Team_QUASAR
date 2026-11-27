package com.quasar.ledger.module.ledger.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.util.MoneyFormatter;
import com.quasar.ledger.config.LedgerProperties;
import org.springframework.stereotype.Component;

/**
 * BR-01/BR-02, FR-TRF-09: an amount is an integer number of minor units from 1 up to the per-transaction limit.
 * Fractions and non-numbers never reach this point - JSON parsing already rejected them with 400.
 */
@Component
public class AmountPolicy {

    private final long maxAmountMinor;

    public AmountPolicy(LedgerProperties properties) {
        this.maxAmountMinor = properties.maxAmountMinor();
    }

    public long validate(Long amountMinor) {
        if (amountMinor == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "amountMinor is required");
        }
        if (amountMinor < 1) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "amountMinor must be at least 1");
        }
        if (amountMinor > maxAmountMinor) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "amountMinor exceeds the per-transaction limit of "
                    + maxAmountMinor + " (" + MoneyFormatter.format(maxAmountMinor, "INR") + ")");
        }
        return amountMinor;
    }

    public long maxAmountMinor() {
        return maxAmountMinor;
    }
}
