package com.quasar.ledger.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base class for end-to-end API tests. The server runs on a random port against a fresh SQLite file
 * (see src/test/resources/config/application.properties). All data is synthetic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiTestBase {

    public static final String ADMIN_EMAIL = "admin@ledger.test";
    public static final String ADMIN_PASSWORD = "test-test-test-test";
    public static final String PASSWORD = "Password-123";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    protected ApiClient api;

    public record TestUser(String accountId, String email, String token, String displayName) {
    }

    public record TestMerchant(String accountId, String email, String token, String merchantCode, String walletId,
                               String businessName) {
    }

    @BeforeEach
    void createClient() {
        api = new ApiClient("http://localhost:" + port, objectMapper);
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + COUNTER.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
    }

    /** A unique, valid Idempotency-Key. */
    protected static String newKey() {
        return "key-" + UUID.randomUUID();
    }

    protected String login(String email, String password) {
        ApiClient.Response r = api.post("/api/v1/auth/login", Map.of("email", email, "password", password), null);
        assertThat(r.status()).as("login %s", r).isEqualTo(200);
        return r.text("accessToken");
    }

    protected String adminToken() {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected TestUser registerUser(String displayName) {
        String email = uniqueEmail("user");
        ApiClient.Response r = api.post("/api/v1/auth/register",
                Map.of("displayName", displayName, "email", email, "password", PASSWORD), null);
        assertThat(r.status()).as("register %s", r).isEqualTo(201);
        return new TestUser(r.text("accountId"), email, login(email, PASSWORD), displayName);
    }

    protected TestMerchant registerMerchant(String businessName) {
        String email = uniqueEmail("merchant");
        ApiClient.Response r = api.post("/api/v1/auth/register/merchant",
                Map.of("businessName", businessName, "email", email, "password", PASSWORD), null);
        assertThat(r.status()).as("register merchant %s", r).isEqualTo(201);
        return new TestMerchant(r.text("accountId"), email, login(email, PASSWORD), r.text("merchantCode"), r.text("walletId"),
                businessName);
    }

    protected String createWallet(String token, String currency) {
        ApiClient.Response r = api.post("/api/v1/wallets", Map.of("currency", currency), token);
        assertThat(r.status()).as("create wallet %s", r).isEqualTo(201);
        return r.text("walletId");
    }

    /** Balance straight from the database: never trust only the HTTP status for money. */
    protected long storedBalance(String accountId) {
        Long b = jdbc.queryForObject("SELECT balance_minor FROM account WHERE id = ?", Long.class, accountId);
        return b == null ? -1 : b;
    }

    protected long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }
}
