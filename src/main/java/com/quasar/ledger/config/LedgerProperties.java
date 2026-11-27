package com.quasar.ledger.config;

import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * All ledger settings (prefix {@code ledger.*}). Values are validated at start-up so a
 * misconfigured deployment fails fast instead of handling money incorrectly.
 */
@ConfigurationProperties(prefix = "ledger")
public record LedgerProperties(
        @DefaultValue Db db,
        @DefaultValue Jwt jwt,
        @DefaultValue Admin admin,
        @DefaultValue("INR") List<String> currencies,
        @DefaultValue("10000000") long maxAmountMinor,
        @DefaultValue("10") int bcryptStrength) {

    public LedgerProperties {
        currencies = currencies.stream().map(c -> c.trim().toUpperCase(Locale.ROOT)).toList();
        if (!currencies.contains("INR")) {
            throw new IllegalStateException("ledger.currencies must include INR (SRS 2.7)");
        }
        for (String c : currencies) {
            if (!c.matches("^[A-Z]{3}$")) {
                throw new IllegalStateException("Invalid currency code in ledger.currencies: " + c);
            }
        }
        if (maxAmountMinor < 1) {
            throw new IllegalStateException("ledger.max-amount-minor must be at least 1");
        }
        if (bcryptStrength < 10 || bcryptStrength > 16) {
            throw new IllegalStateException("ledger.bcrypt-strength must be between 10 and 16 (NFR-SEC-01)");
        }
    }

    public record Db(
            @DefaultValue("financial_ledger.db") String path,
            @DefaultValue("5000") int busyTimeoutMs,
            @DefaultValue("8") int poolSize) {
    }

    public record Jwt(
            String secret,
            @DefaultValue("60") int expiryMinutes,
            @DefaultValue("false") boolean allowEphemeralSecret) {
    }

    public record Admin(
            String email,
            String password,
            @DefaultValue("Platform Admin") String displayName) {
    }
}
