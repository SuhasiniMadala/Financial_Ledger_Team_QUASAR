package com.quasar.ledger.module.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.common.security.Role;
import com.quasar.ledger.config.LedgerProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR-AUTH-06, FR-AUTH-07, NFR-SEC-02. */
class JwtServiceTest {

    private static final String SECRET = "k".repeat(40);
    private static final Instant T0 = Instant.parse("2026-11-27T10:00:00Z");

    private static LedgerProperties props(String secret, boolean ephemeral) {
        return new LedgerProperties(new LedgerProperties.Db("x.db", 5000, 2), new LedgerProperties.Jwt(secret, 60, ephemeral),
                new LedgerProperties.Admin(null, null, "Admin"), List.of("INR"), 10_000_000L, 10);
    }

    @Test
    void tokenCarriesAccountIdAndRoleAndExpiresAfter60Minutes() {
        JwtService jwt = new JwtService(props(SECRET, false), Clock.fixed(T0, ZoneOffset.UTC));
        JwtService.IssuedToken token = jwt.issue("U-ABCDEFGHJK", Role.MERCHANT);
        assertThat(token.expiresInSeconds()).isEqualTo(3600);
        assertThat(jwt.verify(token.token())).contains(new AuthenticatedUser("U-ABCDEFGHJK", Role.MERCHANT));

        JwtService later = new JwtService(props(SECRET, false), Clock.fixed(T0.plus(Duration.ofMinutes(61)), ZoneOffset.UTC));
        assertThat(later.verify(token.token())).as("expired token").isEmpty();
    }

    @Test
    void tamperedOrForeignTokensAreRejected() {
        JwtService jwt = new JwtService(props(SECRET, false), Clock.systemUTC());
        String token = jwt.issue("U-ABCDEFGHJK", Role.USER).token();
        JwtService other = new JwtService(props("z".repeat(40), false), Clock.systemUTC());
        assertThat(other.verify(token)).isEmpty();
        assertThat(jwt.verify(token.substring(0, token.length() - 2) + "xx")).isEmpty();
        assertThat(jwt.verify("not-a-jwt")).isEmpty();
        // "alg: none" token with the same claims must not be accepted.
        String[] parts = token.split("\\.");
        String unsigned = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes())
                + "." + parts[1] + ".";
        assertThat(jwt.verify(unsigned)).isEmpty();
    }

    @Test
    void secretShorterThan256BitsIsRefused() {
        assertThatThrownBy(() -> new JwtService(props("too-short", false), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
    }

    @Test
    void missingSecretIsRefusedUnlessEphemeralIsExplicitlyAllowed() {
        assertThatThrownBy(() -> new JwtService(props(null, false), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("LEDGER_JWT_SECRET");
        JwtService ephemeral = new JwtService(props(null, true), Clock.systemUTC());
        assertThat(ephemeral.verify(ephemeral.issue("U-1", Role.ADMIN).token())).isPresent();
    }
}
