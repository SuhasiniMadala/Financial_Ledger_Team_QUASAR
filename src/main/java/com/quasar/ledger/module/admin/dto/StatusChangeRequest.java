package com.quasar.ledger.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** FR-ADM-05: every status change needs a reason. */
public record StatusChangeRequest(@NotBlank @Size(max = 500) String reason) {
}
