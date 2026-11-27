package com.quasar.ledger.module.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import com.quasar.ledger.module.idempotency.repository.IdempotencyRepository;
import com.quasar.ledger.module.ledger.repository.LedgerEntryRepository;
import com.quasar.ledger.module.wallet.repository.AccountRepository;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

/**
 * NFR-SAFE-01 / NFR-TEST-02 / FR-CON-07 / TQ-2: inject failures part-way through a money operation and prove
 * nothing remains - no transaction, entry, balance change or idempotency record - and that a retry with the
 * same key then succeeds exactly once (SRS 4.3.2 #7). Uses its own Spring context because of the spies.
 */
class RollbackFailureInjectionTest extends ApiTestBase {

    @SpyBean
    private LedgerEntryRepository entries;

    @SpyBean
    private AccountRepository accounts;

    @SpyBean
    private IdempotencyRepository idempotencyRecords;

    private String admin;
    private String wallet;
    private long systemBefore;

    @BeforeEach
    void setUp() {
        admin = adminToken();
        wallet = createWallet(registerUser("Rollback").token(), "INR");
        systemBefore = storedBalance("SYS-FUND-INR");
    }

    @Test
    @DisplayName("Failure after the first entry is written -> full rollback")
    void failureAfterFirstEntry() {
        doCallRealMethod().doThrow(new IllegalStateException("simulated disk full")).when(entries).insert(any());
        assertRolledBackThenRetrySucceeds();
    }

    @Test
    @DisplayName("Failure after the first balance update -> full rollback")
    void failureAfterBalanceUpdate() {
        doCallRealMethod().doThrow(new IllegalStateException("simulated crash")).when(accounts).updateBalance(anyString(), anyLong());
        assertRolledBackThenRetrySucceeds();
    }

    @Test
    @DisplayName("Failure while saving the idempotency record -> full rollback (FR-TRF-07)")
    void failureAtIdempotencyInsert() {
        doThrow(new IllegalStateException("simulated failure")).when(idempotencyRecords).insert(any());
        assertRolledBackThenRetrySucceeds();
    }

    private void assertRolledBackThenRetrySucceeds() {
        String key = newKey();
        Response failed = LedgerSteps.fund(api, admin, wallet, 5_000, key);
        assertThat(failed.status()).isEqualTo(500);
        assertThat(failed.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(failed.raw().body()).doesNotContain("simulated").doesNotContain("Exception");

        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE destination_account_id = ?", wallet)).isZero();
        assertThat(count("SELECT COUNT(*) FROM ledger_entry WHERE account_id = ?", wallet)).isZero();
        assertThat(storedBalance(wallet)).isZero();
        assertThat(storedBalance("SYS-FUND-INR")).isEqualTo(systemBefore);
        assertThat(count("SELECT COUNT(*) FROM idempotency_record WHERE idem_key = ?", key)).isZero();

        org.mockito.Mockito.reset(entries, accounts, idempotencyRecords);
        Response retry = LedgerSteps.fund(api, admin, wallet, 5_000, key);
        assertThat(retry.status()).isEqualTo(201);
        assertThat(retry.header("Idempotent-Replayed")).isNull();
        assertThat(storedBalance(wallet)).isEqualTo(5_000);
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE destination_account_id = ?", wallet)).isEqualTo(1);
        LedgerSteps.assertReconciles(api, admin);
    }
}
