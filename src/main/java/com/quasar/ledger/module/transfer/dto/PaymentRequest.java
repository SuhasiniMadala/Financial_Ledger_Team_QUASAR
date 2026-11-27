package com.quasar.ledger.module.transfer.dto;

import com.quasar.ledger.common.id.Ids;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** FR-PAY-01: pay a merchant (by merchant code) from an own wallet, optional reference up to 64 characters. */
public record PaymentRequest(
        @NotNull @Pattern(regexp = Ids.WALLET_ID, message = "is malformed") String sourceWalletId,
        @NotNull @Pattern(regexp = Ids.MERCHANT_CODE, message = "is malformed") String merchantCode,
        @NotNull Long amountMinor,
        @Size(max = 64) String reference) {
}
