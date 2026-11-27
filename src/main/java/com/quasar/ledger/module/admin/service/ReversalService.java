package com.quasar.ledger.module.admin.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.module.admin.dto.ReversalRequest;
import com.quasar.ledger.module.idempotency.service.FinancialOutcome;
import com.quasar.ledger.module.idempotency.service.IdempotencyKeys;
import com.quasar.ledger.module.idempotency.service.IdempotencyService;
import com.quasar.ledger.module.idempotency.service.IdempotentRequest;
import com.quasar.ledger.module.idempotency.service.IdempotentResponse;
import com.quasar.ledger.module.ledger.dto.TransactionReceipt;
import com.quasar.ledger.module.ledger.model.LedgerEntry;
import com.quasar.ledger.module.ledger.model.NewTransaction;
import com.quasar.ledger.module.ledger.model.Posting;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.repository.LedgerEntryRepository;
import com.quasar.ledger.module.ledger.repository.TransactionRepository;
import com.quasar.ledger.module.ledger.service.LedgerService;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountStatus;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * FR-LED-07..10, ACC-04, BR-10: an admin corrects a COMPLETED TEST_FUNDING, TRANSFER or MERCHANT_PAYMENT by
 * posting a REVERSAL with the same accounts and amounts in the opposite directions. The original is never changed.
 * FROZEN wallets may take part in reversals; DEACTIVATED wallets may not (BR-07).
 */
@Service
public class ReversalService {

    private final IdempotencyService idempotency;
    private final TransactionRepository transactions;
    private final LedgerEntryRepository entries;
    private final AccountRepository accounts;
    private final LedgerService ledger;

    public ReversalService(IdempotencyService idempotency, TransactionRepository transactions, LedgerEntryRepository entries,
                           AccountRepository accounts, LedgerService ledger) {
        this.idempotency = idempotency;
        this.transactions = transactions;
        this.entries = entries;
        this.accounts = accounts;
        this.ledger = ledger;
    }

    public IdempotentResponse reverse(AuthenticatedUser admin, String transactionId, ReversalRequest body, String key, String requestId) {
        IdempotencyKeys.validate(key);
        String path = "/api/v1/admin/transactions/" + transactionId + "/reversal";
        return idempotency.execute(new IdempotentRequest(admin.accountId(), key, "POST", path, body, requestId), () -> {
            TransactionRecord original = transactions.findById(transactionId).orElseThrow(() -> ApiException.notFound("Transaction"));
            if (original.status() == TransactionStatus.FAILED || original.type() == TransactionType.REVERSAL) {
                return FinancialOutcome.rejected(ErrorCode.NOT_REVERSIBLE,
                        "FAILED transactions and reversals cannot be reversed", null);
            }
            transactions.findReversalOf(transactionId).ifPresent(r -> {
                throw new ApiException(ErrorCode.ALREADY_REVERSED, "Transaction was already reversed by " + r.id());
            });

            List<Posting> mirror = new ArrayList<>();
            Map<String, Long> remaining = new HashMap<>();
            for (LedgerEntry e : entries.findByTransaction(transactionId)) {
                Account account = accounts.findById(e.accountId()).orElseThrow();
                if (account.status() == AccountStatus.DEACTIVATED) {
                    return FinancialOutcome.rejected(ErrorCode.WALLET_NOT_ACTIVE, "A wallet in this transaction is DEACTIVATED", null);
                }
                Posting p = new Posting(e.accountId(), e.direction().opposite(), e.amountMinor());
                if (LedgerService.decreases(account.type(), p.direction())) {
                    long left = remaining.getOrDefault(account.id(), account.balanceMinor()) - p.amountMinor();
                    if (left < 0) {
                        return FinancialOutcome.rejected(ErrorCode.INSUFFICIENT_FUNDS,
                                "The reversal would make a balance negative", null);
                    }
                    remaining.put(account.id(), left);
                }
                mirror.add(p);
            }
            TransactionRecord reversal = ledger.postCompleted(
                    new NewTransaction(TransactionType.REVERSAL, original.amountMinor(), original.currency(), admin.accountId(),
                            original.destinationAccountId(), original.sourceAccountId(), original.id(), body.reason().trim()),
                    mirror);
            return FinancialOutcome.completed(TransactionReceipt.of(reversal), reversal.id());
        });
    }
}
