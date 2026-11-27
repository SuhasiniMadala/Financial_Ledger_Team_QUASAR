package com.quasar.ledger.module.admin.service;

import com.quasar.ledger.common.api.PageQuery;
import com.quasar.ledger.common.api.PageResponse;
import com.quasar.ledger.common.security.Role;
import com.quasar.ledger.module.admin.dto.AccountSummaryResponse;
import com.quasar.ledger.module.admin.dto.UserSummaryResponse;
import com.quasar.ledger.module.auth.repository.MerchantProfileRepository;
import com.quasar.ledger.module.auth.repository.UserAccountRepository;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only admin directory (FR-ADM-01, FR-ADM-02). */
@Service
public class AdminDirectoryService {

    private final UserAccountRepository users;
    private final MerchantProfileRepository merchants;
    private final AccountRepository accounts;

    public AdminDirectoryService(UserAccountRepository users, MerchantProfileRepository merchants, AccountRepository accounts) {
        this.users = users;
        this.merchants = merchants;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> users(PageQuery page) {
        var items = users.findByRole(Role.USER, page).stream()
                .map(u -> new UserSummaryResponse(u.id(), u.displayName(), u.email(), u.createdAt(), null))
                .toList();
        return PageResponse.of(items, page, users.countByRole(Role.USER));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> merchants(PageQuery page) {
        var items = merchants.findSummaries(page).stream()
                .map(m -> new UserSummaryResponse(m.accountId(), m.businessName(), m.email(), m.registeredAt(), m.merchantCode()))
                .toList();
        return PageResponse.of(items, page, users.countByRole(Role.MERCHANT));
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountSummaryResponse> accounts(PageQuery page) {
        var items = accounts.findAll(page).stream().map(AccountSummaryResponse::from).toList();
        return PageResponse.of(items, page, accounts.countAll());
    }
}
