package com.quasar.ledger.module.transfer.service;

import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.security.AuthenticatedUser;
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
import com.quasar.ledger.module.transfer.dto.TransferRequest;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountType;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * SRS 4.3 idempotent transfers (FR-TRF-01..14). Validation that needs no data (key, amount, body) happens first
 * and records nothing. Everything else runs in one write-locked database transaction via IdempotencyService.
 */
@Service
public class TransferService {

    public static final String PATH = "/api/v1/transfers";

    private final IdempotencyService idempotency;
    private final AccountRepository accounts;
    private final LedgerService ledger;
    private final AmountPolicy amounts;

    public TransferService(IdempotencyService idempotency, AccountRepository accounts, LedgerService ledger, AmountPolicy amounts) {
        this.idempotency = idempotency;
        this.accounts = accounts;
        this.ledger = ledger;
        this.amounts = amounts;
    }

    public IdempotentResponse transfer(AuthenticatedUser caller, String key, TransferRequest body, String requestId) {
        IdempotencyKeys.validate(key);
        long amount = amounts.validate(body.amountMinor());
        return idempotency.execute(new IdempotentRequest(caller.accountId(), key, "POST", PATH, body, requestId), () -> {
            // FR-TRF-10: a missing source and someone else's source look identical (404) and record nothing.
            Account source = accounts.findById(body.sourceWalletId())
                    .filter(a -> a.isOwnedBy(caller.accountId()) && a.type() == AccountType.USER_WALLET)
                    .orElseThrow(() -> ApiException.notFound("Source wallet"));
            Optional<Account> found = accounts.findById(body.destinationWalletId());
            // BR-04: users transfer only to USER wallets.
            Account payee = found.filter(a -> a.type() == AccountType.USER_WALLET).orElse(null);
            NewTransaction tx = new NewTransaction(TransactionType.TRANSFER, amount, source.currency(), caller.accountId(),
                    source.id(), found.map(Account::id).orElse(null), null, body.description());

            Optional<TransferRules.Violation> violation = TransferRules.check(source, payee, amount);
            if (violation.isPresent()) {
                // FR-TRF-13: every rule rejection leaves a FAILED transaction (no entries) that the user can see.
                TransactionRecord failed = ledger.recordFailed(tx, violation.get().code());
                return FinancialOutcome.rejected(violation.get().code(), violation.get().message(), failed.id());
            }
            TransactionRecord done = ledger.postCompleted(tx,
                    List.of(Posting.debit(source.id(), amount), Posting.credit(payee.id(), amount)));
            return FinancialOutcome.completed(TransactionReceipt.of(done), done.id());
        });
    }
}
