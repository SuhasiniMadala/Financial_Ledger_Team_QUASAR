package com.quasar.ledger.module.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.module.ledger.model.EntryDirection;
import com.quasar.ledger.module.ledger.model.LedgerEntry;
import com.quasar.ledger.module.ledger.model.NewTransaction;
import com.quasar.ledger.module.ledger.model.Posting;
import com.quasar.ledger.module.ledger.model.TransactionRecord;
import com.quasar.ledger.module.ledger.model.TransactionType;
import com.quasar.ledger.module.ledger.repository.LedgerEntryRepository;
import com.quasar.ledger.module.ledger.service.LedgerInvariantViolation;
import com.quasar.ledger.module.ledger.service.LedgerService;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/** The double-entry engine (FR-LED-01..04, ACC-02/03, FR-CON-01, NFR-TEST-01). */
class LedgerServiceTest extends ApiTestBase {

    @Autowired
    private LedgerService ledger;

    @Autowired
    private LedgerEntryRepository entries;

    @Autowired
    private TransactionTemplate writeTransaction;

    private TransactionRecord post(NewTransaction tx, List<Posting> postings) {
        return writeTransaction.execute(s -> ledger.postCompleted(tx, postings));
    }

    @Test
    @DisplayName("ACC-03 / FR-LED-01/04: funding posts DEBIT System Funding, CREDIT wallet, with balance-after and sequence")
    void balancedPostingWritesEntriesAndBalances() {
        TestUser asha = registerUser("Asha Rao");
        String wallet = createWallet(asha.token(), "INR");
        long systemBefore = storedBalance("SYS-FUND-INR");
        TransactionRecord t = post(new NewTransaction(TransactionType.TEST_FUNDING, 500_000, "INR", asha.accountId(), "SYS-FUND-INR",
                wallet, null, null), List.of(Posting.debit("SYS-FUND-INR", 500_000), Posting.credit(wallet, 500_000)));

        assertThat(storedBalance(wallet)).isEqualTo(500_000);
        assertThat(storedBalance("SYS-FUND-INR")).isEqualTo(systemBefore + 500_000);
        List<LedgerEntry> lines = entries.findByTransaction(t.id());
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).direction()).isEqualTo(EntryDirection.DEBIT);
        assertThat(lines.get(0).balanceAfterMinor()).isEqualTo(systemBefore + 500_000);
        assertThat(lines.get(1).direction()).isEqualTo(EntryDirection.CREDIT);
        assertThat(lines.get(1).balanceAfterMinor()).isEqualTo(500_000);
        assertThat(lines.get(1).sequenceNo()).isGreaterThan(lines.get(0).sequenceNo());
        assertThat(lines).allSatisfy(e -> assertThat(e.createdAt()).isEqualTo(t.createdAt()));
    }

    @Test
    @DisplayName("FR-LED-02 (SRS 4.6.2 #2): DEBIT 100000 / CREDIT 99999 is refused before anything is written")
    void unbalancedPostingIsRejectedAndNothingIsWritten() {
        TestUser asha = registerUser("Asha Rao");
        TestUser ravi = registerUser("Ravi Kumar");
        String a = createWallet(asha.token(), "INR");
        String b = createWallet(ravi.token(), "INR");
        LedgerSteps.fundOk(api, adminToken(), a, 200_000);
        long txBefore = count("SELECT COUNT(*) FROM transaction_record");
        long entriesBefore = count("SELECT COUNT(*) FROM ledger_entry");

        assertThatThrownBy(() -> post(new NewTransaction(TransactionType.TRANSFER, 100_000, "INR", asha.accountId(), a, b, null, null),
                List.of(Posting.debit(a, 100_000), Posting.credit(b, 99_999))))
                .isInstanceOf(LedgerInvariantViolation.class).hasMessageContaining("Unbalanced");

        assertThat(count("SELECT COUNT(*) FROM transaction_record")).isEqualTo(txBefore);
        assertThat(count("SELECT COUNT(*) FROM ledger_entry")).isEqualTo(entriesBefore);
        assertThat(storedBalance(a)).isEqualTo(200_000);
        assertThat(storedBalance(b)).isZero();
    }

    @Test
    void otherInvariantViolationsAreRejected() {
        TestUser asha = registerUser("Asha Rao");
        String inr = createWallet(asha.token(), "INR");
        String usd = createWallet(asha.token(), "USD");
        NewTransaction tx = new NewTransaction(TransactionType.TEST_FUNDING, 100, "INR", asha.accountId(), "SYS-FUND-INR", inr, null, null);
        assertThatThrownBy(() -> post(tx, List.of(Posting.credit(inr, 100))))
                .isInstanceOf(LedgerInvariantViolation.class).hasMessageContaining("two entries");
        assertThatThrownBy(() -> post(tx, List.of(Posting.debit("SYS-FUND-INR", 100), Posting.credit(usd, 100))))
                .isInstanceOf(LedgerInvariantViolation.class).hasMessageContaining("currency");
        assertThatThrownBy(() -> post(tx, List.of(Posting.debit("SYS-FUND-INR", 0), Posting.credit(inr, 0))))
                .isInstanceOf(LedgerInvariantViolation.class);
        // FR-CON-05: debiting a wallet below zero is impossible even if a caller forgot the funds check.
        NewTransaction spend = new NewTransaction(TransactionType.TRANSFER, 100, "INR", asha.accountId(), inr, "SYS-FUND-INR", null, null);
        assertThatThrownBy(() -> post(spend, List.of(Posting.debit(inr, 100), Posting.credit("SYS-FUND-INR", 100))))
                .isInstanceOf(LedgerInvariantViolation.class).hasMessageContaining("negative");
        assertThat(storedBalance(inr)).isZero();
    }

    @Test
    @DisplayName("FR-CON-01: the ledger refuses to run outside a database transaction")
    void requiresSurroundingTransaction() {
        TestUser asha = registerUser("Asha Rao");
        String inr = createWallet(asha.token(), "INR");
        assertThatThrownBy(() -> ledger.postCompleted(new NewTransaction(TransactionType.TEST_FUNDING, 1, "INR", asha.accountId(),
                "SYS-FUND-INR", inr, null, null), List.of(Posting.debit("SYS-FUND-INR", 1), Posting.credit(inr, 1))))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("FR-LED-03 / FR-TRF-13: a FAILED transaction has a failure code and no entries")
    void failedTransactionHasNoEntries() {
        TestUser asha = registerUser("Asha Rao");
        String inr = createWallet(asha.token(), "INR");
        TransactionRecord failed = writeTransaction.execute(s -> ledger.recordFailed(new NewTransaction(TransactionType.TRANSFER,
                600_000, "INR", asha.accountId(), inr, null, null, null), ErrorCode.INSUFFICIENT_FUNDS));
        assertThat(failed.status().name()).isEqualTo("FAILED");
        assertThat(failed.failureCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(entries.findByTransaction(failed.id())).isEmpty();
    }
}
