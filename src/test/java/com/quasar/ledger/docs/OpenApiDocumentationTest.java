package com.quasar.ledger.docs;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SE-7 / NFR-MAIN-04: the generated OpenAPI 3 document describes every endpoint of SRS Table 3-1, the
 * Idempotency-Key header on starred endpoints and the error schema. It is written to target/openapi/openapi.json;
 * docs/api/openapi.json is a committed copy (see docs/api/README.md for how to refresh it).
 */
class OpenApiDocumentationTest extends ApiTestBase {

    private static final String[][] TABLE_3_1 = {
        {"post", "/api/v1/auth/register"}, {"post", "/api/v1/auth/register/merchant"}, {"post", "/api/v1/auth/login"},
        {"get", "/api/v1/health"}, {"post", "/api/v1/wallets"}, {"get", "/api/v1/wallets"}, {"get", "/api/v1/wallets/{id}"},
        {"post", "/api/v1/transfers"}, {"post", "/api/v1/payments"}, {"get", "/api/v1/transactions"},
        {"get", "/api/v1/transactions/{id}"}, {"get", "/api/v1/merchant/account"}, {"get", "/api/v1/merchant/transactions"},
        {"get", "/api/v1/merchant/statistics"}, {"get", "/api/v1/admin/users"}, {"get", "/api/v1/admin/merchants"},
        {"get", "/api/v1/admin/accounts"}, {"get", "/api/v1/admin/accounts/{id}/entries"}, {"get", "/api/v1/admin/transactions"},
        {"get", "/api/v1/admin/transactions/{id}"}, {"post", "/api/v1/admin/wallets/{id}/freeze"},
        {"post", "/api/v1/admin/wallets/{id}/unfreeze"}, {"post", "/api/v1/admin/wallets/{id}/deactivate"},
        {"post", "/api/v1/admin/wallets/{id}/test-funding"}, {"post", "/api/v1/admin/transactions/{id}/reversal"},
        {"post", "/api/v1/admin/reconciliations"}, {"get", "/api/v1/admin/statistics"}};

    @Test
    @DisplayName("Every Table 3-1 endpoint is documented; starred endpoints declare the Idempotency-Key header")
    void openApiCoversTable31() throws Exception {
        Response r = api.get("/v3/api-docs", null);
        assertThat(r.status()).isEqualTo(200);
        Path out = Files.createDirectories(Path.of("target", "openapi")).resolve("openapi.json");
        Files.writeString(out, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(r.body()));
        JsonNode paths = r.body().get("paths");
        List<String> missing = new ArrayList<>();
        for (String[] e : TABLE_3_1) {
            if (paths.path(e[1]).path(e[0]).isMissingNode()) {
                missing.add(e[0].toUpperCase() + " " + e[1]);
            }
        }
        assertThat(missing).as("endpoints missing from OpenAPI").isEmpty();
        assertThat(paths.size()).as("no undocumented extra endpoints beyond Table 3-1").isEqualTo(26);
        for (String starred : new String[] {"/api/v1/transfers", "/api/v1/payments", "/api/v1/admin/wallets/{id}/test-funding",
            "/api/v1/admin/transactions/{id}/reversal"}) {
            JsonNode op = paths.get(starred).get("post");
            // Regression DEF-003: header listed exactly once; success documented as 201, not 200.
            assertThat(op.get("parameters").findValuesAsText("name")).containsOnlyOnce("Idempotency-Key");
            assertThat(op.get("responses").has("201")).as("%s documents 201", starred).isTrue();
            assertThat(op.get("responses").has("200")).as("%s must not document 200", starred).isFalse();
            assertThat(op.get("responses").has("422")).isTrue();
            assertThat(op.get("responses").has("503")).isTrue();
        }
        // Regression DEF-003: every $ref must point at a schema that exists.
        String json = r.raw().body();
        java.util.regex.Matcher refs = java.util.regex.Pattern.compile("#/components/schemas/([A-Za-z0-9_]+)").matcher(json);
        while (refs.find()) {
            assertThat(r.body().get("components").get("schemas").has(refs.group(1))).as("dangling $ref %s", refs.group(1)).isTrue();
        }
        assertThat(r.body().get("components").get("schemas").has("ErrorResponse")).isTrue();
    }
}
