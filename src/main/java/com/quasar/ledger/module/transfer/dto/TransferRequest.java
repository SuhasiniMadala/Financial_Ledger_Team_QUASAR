package com.quasar.ledger.module.transfer.dto;

import com.quasar.ledger.common.id.Ids;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** FR-TRF-01: user-to-user transfer of an integer amount in minor units, optional description up to 140 characters. */
public record TransferRequest(
        @NotNull @Pattern(regexp = Ids.WALLET_ID, message = "is malformed") String sourceWalletId,
        @NotNull @Pattern(regexp = Ids.WALLET_ID, message = "is malformed") String destinationWalletId,
        @NotNull Long amountMinor,
        @Size(max = 140) String description) {
}
