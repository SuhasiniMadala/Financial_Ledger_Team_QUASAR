package com.quasar.ledger.module.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import com.quasar.ledger.module.ledger.repository.LedgerEntryRepository;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import com.quasar.ledger.support.TransferSteps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;

/** SRS 4.3.2 #7 / FR-CON-07: the database fails while writing the CREDIT entry of a transfer. */
class TransferRollbackTest extends ApiTestBase {

    @SpyBean
    private LedgerEntryRepository entries;

    @Test
    @DisplayName("Failure while writing the credit entry -> 500, no record/entry/balance change/saved key; retry is safe")
    void failureWhileWritingCreditEntry() {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        String a = createWallet(asha.token(), "INR");
        String b = createWallet(registerUser("Ravi Kumar").token(), "INR");
        LedgerSteps.fundOk(api, admin, a, 500_000);
        long transactionsBefore = count("SELECT COUNT(*) FROM transaction_record");
        String key = newKey();

        // The DEBIT entry is written for real, then the CREDIT entry fails ("disk full").
        doCallRealMethod().doThrow(new IllegalStateException("disk full")).when(entries).insert(any());
        Response failed = TransferSteps.transfer(api, asha.token(), a, b, 100_000, key);
        assertThat(failed.status()).isEqualTo(500);
        assertThat(failed.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(count("SELECT COUNT(*) FROM transaction_record")).isEqualTo(transactionsBefore);
        assertThat(count("SELECT COUNT(*) FROM ledger_entry WHERE account_id = ?", b)).isZero();
        assertThat(storedBalance(a)).isEqualTo(500_000);
        assertThat(storedBalance(b)).isZero();
        assertThat(count("SELECT COUNT(*) FROM idempotency_record WHERE idem_key = ?", key)).isZero();

        Mockito.reset(entries);
        Response retry = TransferSteps.transfer(api, asha.token(), a, b, 100_000, key);
        assertThat(retry.status()).isEqualTo(201);
        assertThat(storedBalance(a)).isEqualTo(400_000);
        assertThat(storedBalance(b)).isEqualTo(100_000);
        LedgerSteps.assertReconciles(api, admin);
    }
}
