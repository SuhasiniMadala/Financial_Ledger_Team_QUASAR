package com.quasar.ledger.module.ledger.service;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.id.IdGenerator;
import com.quasar.ledger.common.id.IdKind;
import com.quasar.ledger.common.util.Timestamps;
import com.quasar.ledger.module.ledger.model.EntryDirection;
import com.quasar.ledger.module.ledger.model.LedgerEntry;
import com.quasar.ledger.module.ledger.model.NewTransaction;
import com.quasar.ledger.module.ledger.model.Posting;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.model.TransactionStatus;
import com.quasar.ledger.module.ledger.repository.LedgerEntryRepository;
import com.quasar.ledger.module.ledger.repository.TransactionRepository;
import com.quasar.ledger.module.wallet.model.Account;
import com.quasar.ledger.module.wallet.model.AccountType;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sole writer of ledger entries and balances (ADR-003, NFR-MAIN-02).
 *
 * <p>Every method must run inside the caller's read-write database transaction
 * ({@code Propagation.MANDATORY}, FR-CON-01): the caller has already taken the write lock, checked
 * business rules and read balances inside that same transaction (FR-CON-02/03). If anything here
 * throws, the caller's whole transaction rolls back (FR-CON-07).
 */
@Service
public class LedgerService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final LedgerEntryRepository entries;
    private final IdGenerator ids;
    private final Clock clock;

    public LedgerService(AccountRepository accounts, TransactionRepository transactions, LedgerEntryRepository entries,
                         IdGenerator ids, Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.entries = entries;
        this.ids = ids;
        this.clock = clock;
    }

    /**
     * Records a COMPLETED transaction with its entries and updates the cached balances (FR-TRF-14, FR-PAY-03, FR-LED-01).
     * The debit = credit check happens before anything is written (FR-LED-02).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public TransactionRecord postCompleted(NewTransaction tx, List<Posting> postings) {
        Plan plan = validate(tx, postings);
        String now = Timestamps.format(clock.instant());
        TransactionRecord record = new TransactionRecord(ids.generate(IdKind.TRANSACTION), tx.type(), TransactionStatus.COMPLETED,
                tx.amountMinor(), tx.currency(), tx.initiatedBy(), tx.sourceAccountId(), tx.destinationAccountId(),
                tx.reversalOfId(), null, tx.description(), now);
        transactions.insert(record);
        for (int i = 0; i < postings.size(); i++) {
            Posting p = postings.get(i);
            entries.insert(new LedgerEntry(0L, ids.generate(IdKind.LEDGER_ENTRY), record.id(), p.accountId(), p.direction(),
                    p.amountMinor(), plan.balanceAfterEach().get(i), now));
        }
        plan.finalBalances().forEach(accounts::updateBalance);
        return record;
    }

    /** The computed result of a valid posting list: balance after each entry, and each account's final balance. */
    private record Plan(List<Long> balanceAfterEach, Map<String, Long> finalBalances) {
    }

    /** Records a FAILED transaction with its failure code and no ledger entries (FR-TRF-13, FR-LED-03). */
    @Transactional(propagation = Propagation.MANDATORY)
    public TransactionRecord recordFailed(NewTransaction tx, ErrorCode failureCode) {
        TransactionRecord record = new TransactionRecord(ids.generate(IdKind.TRANSACTION), tx.type(), TransactionStatus.FAILED,
                tx.amountMinor(), tx.currency(), tx.initiatedBy(), tx.sourceAccountId(), tx.destinationAccountId(),
                tx.reversalOfId(), failureCode.name(), tx.description(), Timestamps.format(clock.instant()));
        transactions.insert(record);
        return record;
    }

    /** True if posting {@code direction} on this account lowers its balance (debit on a wallet, credit on System Funding). */
    public static boolean decreases(AccountType type, EntryDirection direction) {
        return type.isDebitNormal() ? direction == EntryDirection.CREDIT : direction == EntryDirection.DEBIT;
    }

    /** Validates all invariants and computes the resulting balances. Writes nothing. */
    private Plan validate(NewTransaction tx, List<Posting> postings) {
        if (postings == null || postings.size() < 2) {
            throw new LedgerInvariantViolation("A completed transaction needs at least two entries");
        }
        long debits = 0;
        long credits = 0;
        Map<String, Long> balances = new LinkedHashMap<>();
        List<Long> afterEach = new ArrayList<>();
        for (Posting p : postings) {
            if (p.amountMinor() <= 0) {
                throw new LedgerInvariantViolation("Entry amounts must be positive");
            }
            if (p.direction() == EntryDirection.DEBIT) {
                debits = Math.addExact(debits, p.amountMinor());
            } else {
                credits = Math.addExact(credits, p.amountMinor());
            }
            Account account = accounts.findById(p.accountId())
                    .orElseThrow(() -> new LedgerInvariantViolation("Unknown account " + p.accountId()));
            if (!account.currency().equals(tx.currency())) {
                throw new LedgerInvariantViolation("Account currency does not match transaction currency");
            }
            long before = balances.getOrDefault(p.accountId(), account.balanceMinor());
            long after = applyPosting(account.type(), before, p);
            if (after < 0) {
                throw new LedgerInvariantViolation("Posting would make account " + p.accountId() + " negative");
            }
            balances.put(p.accountId(), after);
            afterEach.add(after);
        }
        if (debits != credits) {
            throw new LedgerInvariantViolation("Unbalanced transaction: debits " + debits + " != credits " + credits);
        }
        if (debits != tx.amountMinor()) {
            throw new LedgerInvariantViolation("Transaction amount does not equal its total debits");
        }
        return new Plan(afterEach, balances);
    }

    /** ACC-02: wallets are credit-normal (liability); System Funding is debit-normal (asset). */
    private static long applyPosting(AccountType type, long balance, Posting p) {
        return decreases(type, p.direction())
                ? Math.subtractExact(balance, p.amountMinor())
                : Math.addExact(balance, p.amountMinor());
    }
}
