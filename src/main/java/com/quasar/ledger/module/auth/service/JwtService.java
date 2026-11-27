package com.quasar.ledger.module.auth.service;

import com.quasar.ledger.common.security.AuthenticatedUser;
import com.quasar.ledger.common.security.Role;
import com.quasar.ledger.config.LedgerProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies JWTs (FR-AUTH-06/07, NFR-SEC-02, ADR-006): HMAC-SHA256, a secret of at least
 * 256 bits read from the LEDGER_JWT_SECRET environment variable, 60-minute expiry.
 * Tests and local checks may opt in to a random per-process secret instead of a stored one.
 */
@Service
public class JwtService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiry;
    private final Clock clock;

    public JwtService(LedgerProperties properties, Clock clock) {
        this.clock = clock;
        this.expiry = Duration.ofMinutes(properties.jwt().expiryMinutes());
        String secret = properties.jwt().secret();
        byte[] keyBytes;
        if (secret == null || secret.isBlank()) {
            if (!properties.jwt().allowEphemeralSecret()) {
                throw new IllegalStateException("Environment variable LEDGER_JWT_SECRET must be set to at least 32 characters "
                        + "(256 bits). See README.md 'Configuration'.");
            }
            keyBytes = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(keyBytes);
            LOG.warn("Using a random JWT secret for this process only (ledger.jwt.allow-ephemeral-secret=true). "
                    + "Tokens become invalid on restart. Never use this in a shared deployment.");
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException("LEDGER_JWT_SECRET is too short: at least 32 bytes (256 bits) are required");
            }
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public record IssuedToken(String token, long expiresInSeconds) {
    }

    public IssuedToken issue(String accountId, Role role) {
        Instant now = clock.instant();
        String token = Jwts.builder()
                .subject(accountId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiry.toSeconds());
    }

    /** Returns the caller for a valid, unexpired token; empty for anything else (FR-AUTH-07). */
    public Optional<AuthenticatedUser> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String subject = claims.getSubject();
            Object role = claims.get("role");
            if (subject == null || role == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(subject, Role.valueOf(role.toString())));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
