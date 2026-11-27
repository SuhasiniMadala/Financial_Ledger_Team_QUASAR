package com.quasar.ledger.module.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import com.quasar.ledger.support.TransferSteps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SRS 4.6.2 #6 and FR-LED-11/12: what a user can see about transactions. */
class TransactionHistoryApiTest extends ApiTestBase {

    private String admin;
    private TestUser asha;
    private TestUser ravi;
    private String ashaWallet;
    private String raviWallet;
    private String transfer;

    @BeforeEach
    void setUp() {
        admin = adminToken();
        asha = registerUser("Asha Rao");
        ravi = registerUser("Ravi Kumar");
        ashaWallet = createWallet(asha.token(), "INR");
        raviWallet = createWallet(ravi.token(), "INR");
        LedgerSteps.fundOk(api, admin, ashaWallet, 500_000);
        transfer = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 100_000, newKey()).text("transactionId");
    }

    @Test
    @DisplayName("FR-LED-11: the recipient sees incoming money; lists are newest first")
    void recipientSeesIncomingTransfer() {
        Response r = api.get("/api/v1/transactions", ravi.token());
        assertThat(r.status()).isEqualTo(200);
        JsonNode first = r.body().get("items").get(0);
        assertThat(first.get("transactionId").asText()).isEqualTo(transfer);
        assertThat(first.get("direction").asText()).isEqualTo("INCOMING");
        assertThat(first.get("counterpartyName").asText()).isEqualTo("Asha Rao");

        Response mine = api.get("/api/v1/transactions", asha.token());
        assertThat(mine.body().get("items").get(0).get("transactionId").asText()).isEqualTo(transfer);
        assertThat(mine.body().get("items").get(0).get("direction").asText()).isEqualTo("OUTGOING");
        assertThat(mine.body().get("items").get(1).get("type").asText()).isEqualTo("TEST_FUNDING");
        assertThat(mine.body().get("items").get(1).get("counterpartyName").asText()).isEqualTo("System Funding");
    }

    @Test
    @DisplayName("FR-LED-11: FAILED attempts are visible only to the user who made them")
    void failedAttemptsAreVisibleOnlyToInitiator() {
        String failed = TransferSteps.transfer(api, asha.token(), ashaWallet, raviWallet, 9_000_000, newKey()).text("transactionId");
        assertThat(api.get("/api/v1/transactions?status=FAILED", asha.token()).body().get("items").findValuesAsText("transactionId"))
                .containsExactly(failed);
        assertThat(api.get("/api/v1/transactions?status=FAILED", ravi.token()).body().get("totalItems").asLong()).isZero();
        assertThat(api.get("/api/v1/transactions/" + failed, ravi.token()).status()).isEqualTo(404);
        JsonNode detail = api.get("/api/v1/transactions/" + failed, asha.token()).body();
        assertThat(detail.get("transaction").get("failureCode").asText()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(detail.get("ownEntries")).isEmpty();
    }

    @Test
    @DisplayName("SRS 4.6.2 #6 / FR-LED-12: detail shows own entry, counterparty name and reversal link - never the other side")
    void detailShowsOnlyOwnSide() {
        String reversal = LedgerSteps.reverse(api, admin, transfer, "sent to wrong recipient", newKey()).text("transactionId");
        Response r = api.get("/api/v1/transactions/" + transfer, asha.token());
        assertThat(r.status()).isEqualTo(200);
        JsonNode t = r.body().get("transaction");
        assertThat(t.get("type").asText()).isEqualTo("TRANSFER");
        assertThat(t.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(t.get("amountMinor").asLong()).isEqualTo(100_000);
        assertThat(t.get("counterpartyName").asText()).isEqualTo("Ravi Kumar");
        assertThat(t.get("reversedBy").asText()).isEqualTo(reversal);
        JsonNode own = r.body().get("ownEntries");
        assertThat(own).hasSize(1);
        assertThat(own.get(0).get("accountId").asText()).isEqualTo(ashaWallet);
        assertThat(r.raw().body()).doesNotContain(raviWallet);
    }

    @Test
    @DisplayName("FR-AUTH-09: another user's transaction is 404; filters and paging are validated")
    void accessAndFilters() {
        TestUser mallory = registerUser("Mallory");
        assertThat(api.get("/api/v1/transactions/" + transfer, mallory.token()).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/transactions?walletId=" + raviWallet, asha.token()).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/transactions?walletId=" + ashaWallet + "&type=TRANSFER", asha.token()).body().get("totalItems")
                .asLong()).isEqualTo(1);
        assertThat(api.get("/api/v1/transactions?type=BOGUS", asha.token()).status()).isEqualTo(400);
        assertThat(api.get("/api/v1/transactions?size=101", asha.token()).status()).isEqualTo(400);
        assertThat(api.get("/api/v1/transactions?size=1", asha.token()).body().get("items")).hasSize(1);
        assertThat(api.get("/api/v1/transactions/T-000000000000", asha.token()).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/transactions/bad", asha.token()).status()).isEqualTo(400);
    }
}
