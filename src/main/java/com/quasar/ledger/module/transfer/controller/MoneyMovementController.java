package com.quasar.ledger.module.transfer.controller;

import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.common.web.RequestIdFilter;
import com.quasar.ledger.module.transfer.dto.PaymentRequest;
import com.quasar.ledger.module.transfer.dto.TransferRequest;
import com.quasar.ledger.module.transfer.service.PaymentService;
import com.quasar.ledger.module.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** POST /transfers and POST /payments (USER only). Both require an Idempotency-Key header. */
@RestController
@Tag(name = "Transfers and payments (USER)")
public class MoneyMovementController {

    private final TransferService transfers;
    private final PaymentService payments;

    public MoneyMovementController(TransferService transfers, PaymentService payments) {
        this.transfers = transfers;
        this.payments = payments;
    }

    @PostMapping(TransferService.PATH)
    @Operation(summary = "Transfer to another USER wallet in the same currency (FR-TRF-01..14). 201 on success; 422 with a "
            + "FAILED transaction ID when a business rule rejects it; a retry with the same key replays the stored response.")
    public ResponseEntity<String> transfer(@AuthenticationPrincipal AuthenticatedUser caller,
                                           @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                           @Valid @RequestBody TransferRequest body, HttpServletRequest request) {
        return transfers.transfer(caller, key, body, RequestIdFilter.requestId(request)).toResponseEntity();
    }

    @PostMapping(PaymentService.PATH)
    @Operation(summary = "Pay a merchant by merchant code (FR-PAY-01..03). Same rules and responses as transfers.")
    public ResponseEntity<String> pay(@AuthenticationPrincipal AuthenticatedUser caller,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                      @Valid @RequestBody PaymentRequest body, HttpServletRequest request) {
        return payments.pay(caller, key, body, RequestIdFilter.requestId(request)).toResponseEntity();
    }
}
