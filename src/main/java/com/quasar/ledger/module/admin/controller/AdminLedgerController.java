package com.quasar.ledger.module.admin.controller;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.api.PageResponse;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.id.Ids;
import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.common.util.DateRange;
import com.quasar.ledger.common.web.RequestIdFilter;
import com.quasar.ledger.module.admin.dto.ReversalRequest;
import com.quasar.ledger.module.admin.dto.StatisticsResponse;
import com.quasar.ledger.module.admin.dto.TestFundingRequest;
import com.quasar.ledger.module.admin.dto.TransactionDetailResponse;
import com.quasar.ledger.module.admin.service.AdminLedgerQueryService;
import com.quasar.ledger.module.admin.service.AdminStatisticsService;
import com.quasar.ledger.module.admin.service.FundingService;
import com.quasar.ledger.module.admin.service.ReversalService;
import com.quasar.ledger.module.ledger.dto.LedgerEntryView;
import com.quasar.ledger.module.ledger.dto.TransactionView;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.repository.TransactionSearch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin: funding, reversals and ledger")
public class AdminLedgerController {

    private final FundingService funding;
    private final ReversalService reversals;
    private final AdminLedgerQueryService queries;
    private final AdminStatisticsService statistics;

    public AdminLedgerController(FundingService funding, ReversalService reversals, AdminLedgerQueryService queries,
                                 AdminStatisticsService statistics) {
        this.funding = funding;
        this.reversals = reversals;
        this.queries = queries;
        this.statistics = statistics;
    }

    @PostMapping("/wallets/{id}/test-funding")
    @Operation(summary = "Credit an ACTIVE USER wallet from the System Funding Account (FR-WAL-07/08). Requires Idempotency-Key.")
    public ResponseEntity<String> testFunding(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable("id") String id,
                                              @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                              @Valid @RequestBody TestFundingRequest body, HttpServletRequest request) {
        Ids.require(id, Ids.WALLET_ID, "wallet id");
        return funding.fund(admin, id, body, key, RequestIdFilter.requestId(request)).toResponseEntity();
    }

    @PostMapping("/transactions/{id}/reversal")
    @Operation(summary = "Reverse a COMPLETED transaction with mirror-image entries (FR-LED-07..10). Requires Idempotency-Key.")
    public ResponseEntity<String> reverse(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable("id") String id,
                                          @RequestHeader(value = "Idempotency-Key", required = false) String key,
                                          @Valid @RequestBody ReversalRequest body, HttpServletRequest request) {
        Ids.require(id, Ids.TRANSACTION_ID, "transaction id");
        return reversals.reverse(admin, id, body, key, RequestIdFilter.requestId(request)).toResponseEntity();
    }

    @GetMapping("/transactions")
    @Operation(summary = "List all transactions, newest first, filtered by type, status, date (yyyy-MM-dd) and wallet (FR-ADM-03)")
    public PageResponse<TransactionView> transactions(@RequestParam(required = false) String type,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String walletId,
                                                      @RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        if (walletId != null) {
            Ids.require(walletId, Ids.ACCOUNT_ID, "walletId");
        }
        TransactionSearch search = TransactionSearch.admin(walletId, parse(TransactionType.class, type, "type"),
                parse(TransactionStatus.class, status, "status"), DateRange.parse(from, to));
        return queries.list(search, PageQuery.of(page, size));
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "One transaction with all its ledger entries (FR-ADM-03)")
    public TransactionDetailResponse transaction(@PathVariable("id") String id) {
        return queries.detail(Ids.require(id, Ids.TRANSACTION_ID, "transaction id"));
    }

    @GetMapping("/accounts/{id}/entries")
    @Operation(summary = "Ledger entries of any account, newest first (FR-ADM-03)")
    public PageResponse<LedgerEntryView> entries(@PathVariable("id") String id, @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size) {
        return queries.accountEntries(Ids.require(id, Ids.ACCOUNT_ID, "account id"), PageQuery.of(page, size));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Platform statistics (FR-ADM-07)")
    public StatisticsResponse statistics() {
        return statistics.statistics();
    }

    static <E extends Enum<E>> E parse(Class<E> type, String value, String name) {
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
