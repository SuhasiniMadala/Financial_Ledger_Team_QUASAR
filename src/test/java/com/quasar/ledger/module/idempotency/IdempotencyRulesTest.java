package com.quasar.ledger.module.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;
import com.quasar.ledger.config.LedgerProperties;
import com.quasar.ledger.module.idempotency.service.FinancialOutcome;
import com.quasar.ledger.module.idempotency.service.IdempotencyKeys;
import com.quasar.ledger.module.idempotency.service.RequestFingerprint;
import com.quasar.ledger.module.ledger.service.AmountPolicy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests (NFR-TEST-01): key format, request fingerprinting and amount boundaries. */
class IdempotencyRulesTest {

    @Test
    @DisplayName("FR-TRF-02: a missing key is IDEMPOTENCY_KEY_REQUIRED")
    void missingKey() {
        assertThatThrownBy(() -> IdempotencyKeys.validate(null)).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        assertThatThrownBy(() -> IdempotencyKeys.validate("")).isInstanceOf(ApiException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "has space-1", "bad/slash1", "éééééééé", "semi;colon1"})
    @DisplayName("FR-TRF-03: too short or wrong characters is VALIDATION_ERROR")
    void malformedKeys(String key) {
        assertThatThrownBy(() -> IdempotencyKeys.validate(key)).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("FR-TRF-03: 8 and 64 characters are accepted, 65 is not")
    void keyLengthBoundaries() {
        assertThat(IdempotencyKeys.validate("abcd-12_")).hasSize(8);
        assertThat(IdempotencyKeys.validate("a".repeat(64))).hasSize(64);
        assertThatThrownBy(() -> IdempotencyKeys.validate("a".repeat(65))).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("FR-TRF-04: fingerprint ignores field order but changes with any value, the path or the method")
    void fingerprint() {
        RequestFingerprint fp = new RequestFingerprint();
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("amountMinor", 100000);
        a.put("destinationWalletId", "W-AAAAAAAAAA");
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("destinationWalletId", "W-AAAAAAAAAA");
        b.put("amountMinor", 100000);
        String base = fp.of("POST", "/api/v1/transfers", a);
        assertThat(base).hasSize(64).matches("^[0-9a-f]{64}$");
        assertThat(fp.of("POST", "/api/v1/transfers", b)).isEqualTo(base);
        assertThat(fp.of("POST", "/api/v1/transfers", Map.of("amountMinor", 200000, "destinationWalletId", "W-AAAAAAAAAA")))
                .isNotEqualTo(base);
        assertThat(fp.of("POST", "/api/v1/payments", a)).isNotEqualTo(base);
        assertThat(fp.of("PUT", "/api/v1/transfers", a)).isNotEqualTo(base);
    }

    @Test
    void onlyUnprocessableOutcomesCanBeStoredAsRejections() {
        assertThat(FinancialOutcome.rejected(ErrorCode.INSUFFICIENT_FUNDS, "x", null).status()).isEqualTo(422);
        assertThatThrownBy(() -> FinancialOutcome.rejected(ErrorCode.ALREADY_REVERSED, "x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("BR-02 / FR-TRF-09: amounts from 1 to the limit; 0, negative and limit+1 are rejected")
    void amountBoundaries() {
        AmountPolicy policy = new AmountPolicy(new LedgerProperties(new LedgerProperties.Db("x", 1, 1),
                new LedgerProperties.Jwt(null, 60, true), new LedgerProperties.Admin(null, null, "A"), List.of("INR"), 10_000_000L, 10));
        assertThat(policy.validate(1L)).isEqualTo(1L);
        assertThat(policy.validate(10_000_000L)).isEqualTo(10_000_000L);
        for (Long bad : new Long[] {null, 0L, -1L, -50_000L, 10_000_001L, Long.MAX_VALUE}) {
            assertThatThrownBy(() -> policy.validate(bad)).as("amount %s", bad).isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        }
    }
}
