package com.quasar.ledger.module.transfer.controller;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.api.PageResponse;
import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.common.util.DateRange;
import com.quasar.ledger.module.transfer.dto.MerchantAccountResponse;
import com.quasar.ledger.module.transfer.dto.MerchantStatisticsResponse;
import com.quasar.ledger.module.transfer.dto.MerchantTransactionView;
import com.quasar.ledger.module.transfer.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant")
@Tag(name = "Merchant views (MERCHANT)")
public class MerchantController {

    private final MerchantService merchants;

    public MerchantController(MerchantService merchants) {
        this.merchants = merchants;
    }

    @GetMapping("/account")
    @Operation(summary = "Business name, merchant code, wallet, balance and status (FR-PAY-04)")
    public MerchantAccountResponse account(@AuthenticationPrincipal AuthenticatedUser caller) {
        return merchants.account(caller.accountId());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Payments and reversals on my wallet, newest first, optional from/to (yyyy-MM-dd) (FR-PAY-05/06)")
    public PageResponse<MerchantTransactionView> transactions(@AuthenticationPrincipal AuthenticatedUser caller,
                                                              @RequestParam(required = false) String from,
                                                              @RequestParam(required = false) String to,
                                                              @RequestParam(required = false) Integer page,
                                                              @RequestParam(required = false) Integer size) {
        return merchants.transactions(caller.accountId(), DateRange.parse(from, to), PageQuery.of(page, size));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Count, total and half-even average of completed payments; default last 30 days (FR-PAY-07)")
    public MerchantStatisticsResponse statistics(@AuthenticationPrincipal AuthenticatedUser caller,
                                                 @RequestParam(required = false) String from,
                                                 @RequestParam(required = false) String to) {
        return merchants.statistics(caller.accountId(), from, to);
    }
}
