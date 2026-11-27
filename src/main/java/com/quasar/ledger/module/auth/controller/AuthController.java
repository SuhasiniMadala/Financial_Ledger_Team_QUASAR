package com.quasar.ledger.module.auth.controller;

import com.quasar.ledger.module.auth.dto.LoginRequest;
import com.quasar.ledger.module.auth.dto.LoginResponse;
import com.quasar.ledger.module.auth.dto.RegisterMerchantRequest;
import com.quasar.ledger.module.auth.dto.RegisterUserRequest;
import com.quasar.ledger.module.auth.dto.RegistrationResponse;
import com.quasar.ledger.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication (public)")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a USER account (FR-AUTH-01, FR-AUTH-03, FR-AUTH-04)")
    public RegistrationResponse register(@Valid @RequestBody RegisterUserRequest body) {
        return auth.registerUser(body);
    }

    @PostMapping("/register/merchant")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a MERCHANT account with merchant code and INR wallet (FR-AUTH-02, FR-WAL-02)")
    public RegistrationResponse registerMerchant(@Valid @RequestBody RegisterMerchantRequest body) {
        return auth.registerMerchant(body);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT; every failure returns the same 401 (FR-AUTH-06)")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        return auth.login(body.email(), body.password());
    }
}
