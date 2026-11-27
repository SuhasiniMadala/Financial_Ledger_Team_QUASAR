package com.quasar.ledger.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** FR-AUTH-01 / FR-AUTH-04. There is deliberately no "role" field: ADMIN cannot be created through the API (FR-AUTH-05). */
public record RegisterUserRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password) {

    @Override
    public String toString() {
        return "RegisterUserRequest[displayName=" + displayName + ", email=" + email + "]";
    }
}
