package com.quasar.ledger.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NFR-SEC-03 / FR-AUTH-08: every endpoint of SRS Table 3-1 is called with no token and with each role.
 * The allowed role must get past security (any status except 401/403); every other role must get 403,
 * and an anonymous caller must get 401 (except the public endpoints).
 */
class SecurityAccessMatrixTest extends ApiTestBase {

    private static final String W = "W-0000000000";
    private static final String T = "T-000000000000";

    /** method, path, allowed role ("PUBLIC" = no token needed). Mirrors SRS Table 3-1. */
    private static final List<String[]> TABLE_3_1 = List.of(
            new String[] {"POST", "/api/v1/auth/register", "PUBLIC"},
            new String[] {"POST", "/api/v1/auth/register/merchant", "PUBLIC"},
            new String[] {"POST", "/api/v1/auth/login", "PUBLIC"},
            new String[] {"GET", "/api/v1/health", "PUBLIC"},
            new String[] {"POST", "/api/v1/wallets", "USER"},
            new String[] {"GET", "/api/v1/wallets", "USER"},
            new String[] {"GET", "/api/v1/wallets/" + W, "USER"},
            new String[] {"POST", "/api/v1/transfers", "USER"},
            new String[] {"POST", "/api/v1/payments", "USER"},
            new String[] {"GET", "/api/v1/transactions", "USER"},
            new String[] {"GET", "/api/v1/transactions/" + T, "USER"},
            new String[] {"GET", "/api/v1/merchant/account", "MERCHANT"},
            new String[] {"GET", "/api/v1/merchant/transactions", "MERCHANT"},
            new String[] {"GET", "/api/v1/merchant/statistics", "MERCHANT"},
            new String[] {"GET", "/api/v1/admin/users", "ADMIN"},
            new String[] {"GET", "/api/v1/admin/merchants", "ADMIN"},
            new String[] {"GET", "/api/v1/admin/accounts", "ADMIN"},
            new String[] {"GET", "/api/v1/admin/accounts/SYS-FUND-INR/entries", "ADMIN"},
            new String[] {"GET", "/api/v1/admin/transactions", "ADMIN"},
            new String[] {"GET", "/api/v1/admin/transactions/" + T, "ADMIN"},
            new String[] {"POST", "/api/v1/admin/wallets/" + W + "/freeze", "ADMIN"},
            new String[] {"POST", "/api/v1/admin/wallets/" + W + "/unfreeze", "ADMIN"},
            new String[] {"POST", "/api/v1/admin/wallets/" + W + "/deactivate", "ADMIN"},
            new String[] {"POST", "/api/v1/admin/wallets/" + W + "/test-funding", "ADMIN"},
            new String[] {"POST", "/api/v1/admin/transactions/" + T + "/reversal", "ADMIN"},
            new String[] {"POST", "/api/v1/admin/reconciliations", "ADMIN"},
            new String[] {"GET", "/api/v1/admin/statistics", "ADMIN"});

    private Map<String, String> tokens;

    @BeforeEach
    void tokens() {
        tokens = Map.of("USER", registerUser("Matrix User").token(), "MERCHANT", registerMerchant("Matrix Shop").token(),
                "ADMIN", adminToken());
    }

    @Test
    @DisplayName("Every Table 3-1 endpoint enforces its role")
    void everyEndpointEnforcesItsRole() {
        for (String[] row : TABLE_3_1) {
            String method = row[0];
            String path = row[1];
            String allowed = row[2];
            Object body = "POST".equals(method) ? Map.of() : null;

            Response anonymous = api.send(method, path, body, null, Map.of());
            if ("PUBLIC".equals(allowed)) {
                assertThat(anonymous.status()).as("%s %s anonymous", method, path).isNotIn(401, 403);
                continue;
            }
            assertThat(anonymous.status()).as("%s %s anonymous", method, path).isEqualTo(401);
            for (String role : Set.of("USER", "MERCHANT", "ADMIN")) {
                Response r = api.send(method, path, body, tokens.get(role), Map.of());
                if (role.equals(allowed)) {
                    assertThat(r.status()).as("%s %s as %s -> %s", method, path, role, r).isNotIn(401, 403);
                } else {
                    assertThat(r.status()).as("%s %s as %s -> %s", method, path, role, r).isEqualTo(403);
                    assertThat(r.code()).isEqualTo("FORBIDDEN");
                }
            }
        }
    }
}
