package com.quasar.ledger.common.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quasar.ledger.common.exception.ApiException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdGeneratorTest {

    private final RandomIdGenerator generator = new RandomIdGenerator();

    @Test
    void generatedIdsMatchTheValidationPatterns() {
        for (int i = 0; i < 200; i++) {
            assertThat(generator.generate(IdKind.WALLET)).matches(Ids.WALLET_ID).matches(Ids.ACCOUNT_ID);
            assertThat(generator.generate(IdKind.TRANSACTION)).matches(Ids.TRANSACTION_ID);
            assertThat(generator.generate(IdKind.MERCHANT_CODE)).matches(Ids.MERCHANT_CODE);
        }
        assertThat(Ids.systemFundingId("INR")).isEqualTo("SYS-FUND-INR").matches(Ids.ACCOUNT_ID);
    }

    @Test
    void idsAreUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertThat(seen.add(generator.generate(IdKind.TRANSACTION))).isTrue();
        }
    }

    @Test
    void malformedIdsAreRejectedAsValidationErrors() {
        assertThatThrownBy(() -> Ids.require("W-123", Ids.WALLET_ID, "wallet id"))
                .isInstanceOf(ApiException.class).hasMessageContaining("malformed");
        assertThatThrownBy(() -> Ids.require("'; DROP TABLE account;--", Ids.WALLET_ID, "wallet id"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> Ids.require(null, Ids.WALLET_ID, "wallet id")).isInstanceOf(ApiException.class);
    }
}
