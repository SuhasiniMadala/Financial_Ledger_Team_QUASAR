package com.quasar.ledger.module.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import com.quasar.ledger.support.LedgerSteps;
import com.quasar.ledger.support.TransferSteps;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SRS 4.4: merchant payments and merchant views (FR-PAY-01..07). */
class PaymentAndMerchantApiTest extends ApiTestBase {

    private String admin;
    private TestUser asha;
    private String ashaWallet;
    private TestMerchant cafe;

    @BeforeEach
    void setUp() {
        admin = adminToken();
        asha = registerUser("Asha Rao");
        ashaWallet = createWallet(asha.token(), "INR");
        LedgerSteps.fundOk(api, admin, ashaWallet, 500_000);
        cafe = registerMerchant("Campus Cafe");
    }

    @Test
    @DisplayName("SRS 4.4.2 #1: Asha pays Rs 250 with reference 'Order 1187'; a retry with the same key does not pay twice")
    void payMerchant() {
        String key = newKey();
        Response r = TransferSteps.pay(api, asha.token(), ashaWallet, cafe.merchantCode(), 25_000, "Order 1187", key);
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.text("type")).isEqualTo("MERCHANT_PAYMENT");
        assertThat(r.text("description")).isEqualTo("Order 1187");
        assertThat(storedBalance(ashaWallet)).isEqualTo(475_000);
        assertThat(storedBalance(cafe.walletId())).isEqualTo(25_000);
        Response retry = TransferSteps.pay(api, asha.token(), ashaWallet, cafe.merchantCode(), 25_000, "Order 1187", key);
        assertThat(retry.header("Idempotent-Replayed")).isEqualTo("true");
        assertThat(storedBalance(cafe.walletId())).isEqualTo(25_000);
        LedgerSteps.assertReconciles(api, admin);
    }

    @Test
    @DisplayName("SRS 4.4.2 #2: unknown code -> INVALID_DESTINATION; paying from a USD wallet -> CURRENCY_MISMATCH; FAILED recorded")
    void paymentRejections() {
        Response unknown = TransferSteps.pay(api, asha.token(), ashaWallet, "MER-000000", 1_000, null, newKey());
        assertThat(unknown.status()).isEqualTo(422);
        assertThat(unknown.code()).isEqualTo("INVALID_DESTINATION");
        String usd = createWallet(asha.token(), "USD");
        LedgerSteps.fundOk(api, admin, usd, 5_000);
        Response mismatch = TransferSteps.pay(api, asha.token(), usd, cafe.merchantCode(), 1_000, null, newKey());
        assertThat(mismatch.code()).isEqualTo("CURRENCY_MISMATCH");
        assertThat(count("SELECT COUNT(*) FROM transaction_record WHERE status = 'FAILED' AND type = 'MERCHANT_PAYMENT' "
                + "AND initiated_by = ?", asha.accountId())).isEqualTo(2);
        assertThat(TransferSteps.pay(api, asha.token(), ashaWallet, cafe.merchantCode(), 600_000, null, newKey()).code())
                .isEqualTo("INSUFFICIENT_FUNDS");
        api.post("/api/v1/admin/wallets/" + cafe.walletId() + "/freeze", java.util.Map.of("reason", "merchant review"), admin);
        assertThat(TransferSteps.pay(api, asha.token(), ashaWallet, cafe.merchantCode(), 1_000, null, newKey()).code())
                .isEqualTo("WALLET_NOT_ACTIVE");
        assertThat(storedBalance(cafe.walletId())).isZero();
    }

    @Test
    void paymentInputValidation() {
        assertThat(TransferSteps.pay(api, asha.token(), ashaWallet, cafe.merchantCode(), 1_000, "r".repeat(65), newKey()).status())
                .isEqualTo(400);
        assertThat(TransferSteps.pay(api, asha.token(), ashaWallet, "MER-1", 1_000, null, newKey()).status()).isEqualTo(400);
        assertThat(TransferSteps.pay(api, asha.token(), ashaWallet, cafe.merchantCode(), 1_000, null, null).code())
                .isEqualTo("IDEMPOTENCY_KEY_REQUIRED");
        // BR-04: merchants cannot send money.
        assertThat(TransferSteps.pay(api, cafe.token(), cafe.walletId(), cafe.merchantCode(), 1_000, null, newKey()).status())
                .isEqualTo(403);
    }

    @Test
    @DisplayName("FR-PAY-04: merchant account view")
    void merchantAccount() {
        Response r = api.get("/api/v1/merchant/account", cafe.token());
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.text("businessName")).isEqualTo("Campus Cafe");
        assertThat(r.text("merchantCode")).isEqualTo(cafe.merchantCode());
        assertThat(r.text("walletId")).isEqualTo(cafe.walletId());
        assertThat(r.text("status")).isEqualTo("ACTIVE");
        assertThat(r.number("balanceMinor")).isZero();
    }

    @Test
    @DisplayName("SRS 4.4.2 #3 / FR-PAY-05/06: payments list shows payer name and reference but never the payer's wallet or balance")
    void merchantTransactionsHidePayerDetails() {
        String pay = TransferSteps.pay(api, asha.token(), ashaWallet, cafe.merchantCode(), 25_000, "Order 1187", newKey())
                .text("transactionId");
        LedgerSteps.reverse(api, admin, pay, "customer returned the item", newKey());
        Response r = api.get("/api/v1/merchant/transactions", cafe.token());
        assertThat(r.status()).isEqualTo(200);
        JsonNode items = r.body().get("items");
        assertThat(items).hasSize(2);
        JsonNode reversal = items.get(0);
        JsonNode payment = items.get(1);
        assertThat(reversal.get("type").asText()).isEqualTo("REVERSAL");
        assertThat(reversal.get("direction").asText()).isEqualTo("OUTGOING");
        assertThat(payment.get("transactionId").asText()).isEqualTo(pay);
        assertThat(payment.get("payerName").asText()).isEqualTo("Asha Rao");
        assertThat(payment.get("reference").asText()).isEqualTo("Order 1187");
        assertThat(payment.get("amountFormatted").asText()).isEqualTo("₹250.00");
        assertThat(r.raw().body()).doesNotContain(ashaWallet).doesNotContain("475000").doesNotContain("balance");
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        assertThat(api.get("/api/v1/merchant/transactions?from=" + today + "&to=" + today, cafe.token()).body().get("totalItems")
                .asLong()).isEqualTo(2);
        assertThat(api.get("/api/v1/merchant/transactions?from=2000-01-01&to=2000-01-31", cafe.token()).body().get("totalItems")
                .asLong()).isZero();
    }

    @Test
    @DisplayName("FR-PAY-07: count, total and half-even average; default window is the last 30 days")
    void merchantStatistics() {
        for (long amount : new long[] {100, 100, 101}) {
            assertThat(TransferSteps.pay(api, asha.token(), ashaWallet, cafe.merchantCode(), amount, null, newKey()).status())
                    .isEqualTo(201);
        }
        Response r = api.get("/api/v1/merchant/statistics", cafe.token());
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.number("paymentCount")).isEqualTo(3);
        assertThat(r.number("totalMinor")).isEqualTo(301);
        assertThat(r.number("averageMinor")).isEqualTo(100); // 100.33 -> 100
        assertThat(api.get("/api/v1/merchant/statistics?from=2000-01-01&to=2000-01-31", cafe.token()).number("paymentCount")).isZero();
        assertThat(api.get("/api/v1/merchant/statistics", asha.token()).status()).isEqualTo(403);
    }
}
