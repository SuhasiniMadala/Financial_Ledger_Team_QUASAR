package com.quasar.ledger.module.wallet.controller;

import com.quasar.ledger.common.id.Ids;
import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.module.wallet.dto.CreateWalletRequest;
import com.quasar.ledger.module.wallet.dto.WalletResponse;
import com.quasar.ledger.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets (USER)")
public class WalletController {

    private final WalletService wallets;

    public WalletController(WalletService wallets) {
        this.wallets = wallets;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a wallet in a supported currency (FR-WAL-01, FR-WAL-03, FR-WAL-04)")
    public WalletResponse create(@AuthenticationPrincipal AuthenticatedUser caller, @Valid @RequestBody CreateWalletRequest body) {
        return WalletResponse.from(wallets.createUserWallet(caller.accountId(), body.currency()));
    }

    @GetMapping
    @Operation(summary = "List my wallets with balances (FR-WAL-06)")
    public List<WalletResponse> list(@AuthenticationPrincipal AuthenticatedUser caller) {
        return wallets.listOwn(caller.accountId()).stream().map(WalletResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my wallets; another user's wallet returns 404 (FR-AUTH-09)")
    public WalletResponse get(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable("id") String id) {
        return WalletResponse.from(wallets.getOwn(caller.accountId(), Ids.require(id, Ids.WALLET_ID, "wallet id")));
    }
}
