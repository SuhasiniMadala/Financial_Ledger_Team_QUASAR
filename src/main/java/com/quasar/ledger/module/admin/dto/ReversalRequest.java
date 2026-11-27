package com.quasar.ledger.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** FR-LED-08: a reversal needs a reason of 10-500 characters. */
public record ReversalRequest(@NotBlank @Size(min = 10, max = 500, message = "must be 10 to 500 characters") String reason) {
}
