package com.quasar.ledger.module.transfer.controller;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.api.PageResponse;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.id.Ids;
import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.common.util.DateRange;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.transfer.dto.UserTransactionDetail;
import com.quasar.ledger.module.transfer.dto.UserTransactionView;
import com.quasar.ledger.module.transfer.service.TransactionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transaction history (USER)")
public class TransactionHistoryController {

    private final TransactionHistoryService history;

    public TransactionHistoryController(TransactionHistoryService history) {
        this.history = history;
    }

    @GetMapping
    @Operation(summary = "My transactions, newest first; filters walletId, type, status, from/to (yyyy-MM-dd) (FR-LED-11)")
    public PageResponse<UserTransactionView> list(@AuthenticationPrincipal AuthenticatedUser caller,
                                                  @RequestParam(required = false) String walletId,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        if (walletId != null) {
            Ids.require(walletId, Ids.WALLET_ID, "walletId");
        }
        return history.list(caller.accountId(), walletId, parse(TransactionType.class, type, "type"),
                parse(TransactionStatus.class, status, "status"), DateRange.parse(from, to), PageQuery.of(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One of my transactions with my own entries only (FR-LED-12); otherwise 404 (FR-AUTH-09)")
    public UserTransactionDetail detail(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable("id") String id) {
        return history.detail(caller.accountId(), Ids.require(id, Ids.TRANSACTION_ID, "transaction id"));
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unknown " + name + ": " + value);
        }
    }
}
