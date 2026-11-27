package com.quasar.ledger.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** FR-WAL-07: simulated money in. amountMinor is an integer number of paise (BR-01). */
public record TestFundingRequest(@NotNull Long amountMinor, @Size(max = 140) String description) {
}
