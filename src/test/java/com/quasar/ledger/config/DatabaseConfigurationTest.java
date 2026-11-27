package com.quasar.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quasar.ledger.support.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;

/** DB-01..DB-04: the database settings and constraints are really in force. */
class DatabaseConfigurationTest extends ApiTestBase {

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("DB-02: WAL mode, foreign keys on, synchronous=FULL, busy timeout 5000 ms")
    void pragmasAreApplied() {
        assertThat(jdbc.queryForObject("PRAGMA journal_mode", String.class)).isEqualToIgnoringCase("wal");
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("PRAGMA synchronous", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("PRAGMA busy_timeout", Integer.class)).isEqualTo(5000);
    }

    @Test
    @DisplayName("Regression DEF-001: tests run with the production configuration plus test overrides, not instead of it")
    void testOverridesDoNotShadowMainConfiguration() {
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("always");
        assertThat(environment.getProperty("spring.jackson.deserialization.accept-float-as-int")).isEqualTo("false");
        assertThat(environment.getProperty("spring.jackson.mapper.allow-coercion-of-scalars")).isEqualTo("false");
        assertThat(environment.getProperty("ledger.db.path")).contains("target/test-db/");
    }

    @Test
    @DisplayName("DB-03: balances cannot be negative or non-integer, enforced by the database")
    void monetaryCheckConstraints() {
        String wallet = createWallet(registerUser("Constraint").token(), "INR");
        assertThatThrownBy(() -> jdbc.update("UPDATE account SET balance_minor = -1 WHERE id = ?", wallet))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE account SET balance_minor = 10.5 WHERE id = ?", wallet))
                .isInstanceOf(DataAccessException.class);
        assertThat(storedBalance(wallet)).isZero();
    }

    @Test
    @DisplayName("DB-04: unique (owner, currency) and foreign keys are enforced")
    void uniqueAndForeignKeys() {
        TestUser u = registerUser("Unique");
        createWallet(u.token(), "INR");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO account (id, owner_id, account_type, currency, balance_minor, status, "
                + "created_at) VALUES ('W-DUPLICATE0', ?, 'USER_WALLET', 'INR', 0, 'ACTIVE', 'x')", u.accountId()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO account (id, owner_id, account_type, currency, balance_minor, status, "
                + "created_at) VALUES ('W-NOOWNER000', 'U-DOESNOTEXIST', 'USER_WALLET', 'INR', 0, 'ACTIVE', 'x')"))
                .isInstanceOf(DataAccessException.class);
    }
}
