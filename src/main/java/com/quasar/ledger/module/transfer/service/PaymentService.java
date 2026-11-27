package com.quasar.ledger.module.transfer.service;

import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.module.auth.repository.MerchantProfileRepository;
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
import com.quasar.ledger.module.transfer.dto.PaymentRequest;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountType;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** SRS 4.4 merchant payments (FR-PAY-01..03): same idempotency, validation and concurrency rules as transfers. */
@Service
public class PaymentService {

    public static final String PATH = "/api/v1/payments";

    private final IdempotencyService idempotency;
    private final AccountRepository accounts;
    private final MerchantProfileRepository merchants;
    private final LedgerService ledger;
    private final AmountPolicy amounts;

    public PaymentService(IdempotencyService idempotency, AccountRepository accounts, MerchantProfileRepository merchants,
                          LedgerService ledger, AmountPolicy amounts) {
        this.idempotency = idempotency;
        this.accounts = accounts;
        this.merchants = merchants;
        this.ledger = ledger;
        this.amounts = amounts;
    }

    public IdempotentResponse pay(AuthenticatedUser caller, String key, PaymentRequest body, String requestId) {
        IdempotencyKeys.validate(key);
        long amount = amounts.validate(body.amountMinor());
        return idempotency.execute(new IdempotentRequest(caller.accountId(), key, "POST", PATH, body, requestId), () -> {
            Account source = accounts.findById(body.sourceWalletId())
                    .filter(a -> a.isOwnedBy(caller.accountId()) && a.type() == AccountType.USER_WALLET)
                    .orElseThrow(() -> ApiException.notFound("Source wallet"));
            // BR-04: a payment must go to a MERCHANT wallet identified by the merchant code.
            Optional<Account> merchantWallet = merchants.findByMerchantCode(body.merchantCode())
                    .flatMap(m -> accounts.findByOwner(m.userAccountId()).stream()
                            .filter(a -> a.type() == AccountType.MERCHANT_WALLET).findFirst());
            NewTransaction tx = new NewTransaction(TransactionType.MERCHANT_PAYMENT, amount, source.currency(), caller.accountId(),
                    source.id(), merchantWallet.map(Account::id).orElse(null), null, body.reference());

            Optional<TransferRules.Violation> violation = TransferRules.check(source, merchantWallet.orElse(null), amount);
            if (violation.isPresent()) {
                TransactionRecord failed = ledger.recordFailed(tx, violation.get().code());
                return FinancialOutcome.rejected(violation.get().code(), violation.get().message(), failed.id());
            }
            String payee = merchantWallet.get().id();
            TransactionRecord done = ledger.postCompleted(tx, List.of(Posting.debit(source.id(), amount), Posting.credit(payee, amount)));
            return FinancialOutcome.completed(TransactionReceipt.of(done), done.id());
        });
    }
}
