package com.quasar.ledger.module.admin.controller;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.api.PageResponse;
import com.quasar.ledger.common.id.Ids;
import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.module.admin.dto.AccountSummaryResponse;
import com.quasar.ledger.module.admin.dto.StatusChangeRequest;
import com.quasar.ledger.module.admin.dto.UserSummaryResponse;
import com.quasar.ledger.module.admin.service.AdminDirectoryService;
import com.quasar.ledger.module.wallet.dto.WalletResponse;
import com.quasar.ledger.module.wallet.service.WalletStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin: directory and wallet status")
public class AdminDirectoryController {

    private final AdminDirectoryService directory;
    private final WalletStatusService statusService;

    public AdminDirectoryController(AdminDirectoryService directory, WalletStatusService statusService) {
        this.directory = directory;
        this.statusService = statusService;
    }

    @GetMapping("/users")
    @Operation(summary = "List users (FR-ADM-01)")
    public PageResponse<UserSummaryResponse> users(@RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        return directory.users(PageQuery.of(page, size));
    }

    @GetMapping("/merchants")
    @Operation(summary = "List merchants with merchant codes (FR-ADM-01)")
    public PageResponse<UserSummaryResponse> merchants(@RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size) {
        return directory.merchants(PageQuery.of(page, size));
    }

    @GetMapping("/accounts")
    @Operation(summary = "List all accounts including System Funding Accounts (FR-ADM-02)")
    public PageResponse<AccountSummaryResponse> accounts(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size) {
        return directory.accounts(PageQuery.of(page, size));
    }

    @PostMapping("/wallets/{id}/freeze")
    @Operation(summary = "ACTIVE -> FROZEN (FR-ADM-04, FR-ADM-05)")
    public WalletResponse freeze(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable("id") String id,
                                 @Valid @RequestBody StatusChangeRequest body) {
        return change(admin, id, WalletStatusService.Action.FREEZE, body);
    }

    @PostMapping("/wallets/{id}/unfreeze")
    @Operation(summary = "FROZEN -> ACTIVE (FR-ADM-04, FR-ADM-05)")
    public WalletResponse unfreeze(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable("id") String id,
                                   @Valid @RequestBody StatusChangeRequest body) {
        return change(admin, id, WalletStatusService.Action.UNFREEZE, body);
    }

    @PostMapping("/wallets/{id}/deactivate")
    @Operation(summary = "ACTIVE or FROZEN -> DEACTIVATED, final (FR-ADM-04, FR-ADM-05)")
    public WalletResponse deactivate(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable("id") String id,
                                     @Valid @RequestBody StatusChangeRequest body) {
        return change(admin, id, WalletStatusService.Action.DEACTIVATE, body);
    }

    private WalletResponse change(AuthenticatedUser admin, String id, WalletStatusService.Action action, StatusChangeRequest body) {
        Ids.require(id, Ids.WALLET_ID, "wallet id");
        return WalletResponse.from(statusService.change(id, action, body.reason(), admin.accountId()));
    }
}
