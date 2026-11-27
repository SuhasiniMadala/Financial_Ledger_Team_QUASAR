package com.quasar.ledger.module.admin.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.id.Ids;
import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.module.admin.dto.TestFundingRequest;
import com.quasar.ledger.module.idempotency.service.FinancialOutcome;
import com.quasar.ledger.module.idempotency.service.IdempotencyKeys;
import com.quasar.ledger.module.idempotency.service.IdempotencyService;
import com.quasar.ledger.module.idempotency.service.IdempotentRequest;
import com.quasar.ledger.module.idempotency.service.IdempotentResponse;
import com.quasar.ledger.module.ledger.dto.TransactionReceipt;
import com.quasar.ledger.module.ledger.model.NewTransaction;
import com.quasar.ledger.module.ledger.model.Posting;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.service.AmountPolicy;
import com.quasar.ledger.module.ledger.service.LedgerService;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountType;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * FR-WAL-07/08, ACC-03: the only way money enters the system. DEBIT the System Funding Account of the
 * wallet's currency, CREDIT the wallet. Idempotent (FR-TRF-02..08 apply).
 */
@Service
public class FundingService {

    private final IdempotencyService idempotency;
    private final AccountRepository accounts;
    private final LedgerService ledger;
    private final AmountPolicy amounts;

    public FundingService(IdempotencyService idempotency, AccountRepository accounts, LedgerService ledger, AmountPolicy amounts) {
        this.idempotency = idempotency;
        this.accounts = accounts;
        this.ledger = ledger;
        this.amounts = amounts;
    }

    public IdempotentResponse fund(AuthenticatedUser admin, String walletId, TestFundingRequest body, String key, String requestId) {
        IdempotencyKeys.validate(key);
        long amount = amounts.validate(body.amountMinor());
        String path = "/api/v1/admin/wallets/" + walletId + "/test-funding";
        return idempotency.execute(new IdempotentRequest(admin.accountId(), key, "POST", path, body, requestId), () -> {
            Account wallet = accounts.findById(walletId)
                    .filter(a -> a.type() != AccountType.SYSTEM_FUNDING)
                    .orElseThrow(() -> ApiException.notFound("Wallet"));
            if (wallet.type() != AccountType.USER_WALLET) {
                return FinancialOutcome.rejected(ErrorCode.INVALID_DESTINATION, "Test funding can only credit a USER wallet", null);
            }
            if (!wallet.isActive()) {
                return FinancialOutcome.rejected(ErrorCode.WALLET_NOT_ACTIVE, "Wallet is " + wallet.status(), null);
            }
            String system = Ids.systemFundingId(wallet.currency());
            TransactionRecord t = ledger.postCompleted(
                    new NewTransaction(TransactionType.TEST_FUNDING, amount, wallet.currency(), admin.accountId(), system, wallet.id(),
                            null, body.description()),
                    List.of(Posting.debit(system, amount), Posting.credit(wallet.id(), amount)));
            return FinancialOutcome.completed(TransactionReceipt.of(t), t.id());
        });
    }
}
