package com.quasar.ledger.module.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateWalletRequest(
        @NotNull @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter upper-case currency code") String currency) {
}
