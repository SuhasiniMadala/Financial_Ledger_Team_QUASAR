package com.quasar.ledger.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** FR-AUTH-02: a merchant registers with business name, email and password. */
public record RegisterMerchantRequest(
        @NotBlank @Size(max = 100) String businessName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password) {

    @Override
    public String toString() {
        return "RegisterMerchantRequest[businessName=" + businessName + ", email=" + email + "]";
    }
}
