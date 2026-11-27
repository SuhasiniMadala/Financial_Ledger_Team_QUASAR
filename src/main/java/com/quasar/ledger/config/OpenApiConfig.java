package com.quasar.ledger.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 description (SE-7, NFR-MAIN-04): bearer auth, the shared ErrorResponse schema, the
 * Idempotency-Key header on every starred endpoint of Table 3-1, and the error codes each operation can return.
 */
@Configuration
public class OpenApiConfig {

    /** Endpoints marked with a star in SRS Table 3-1. */
    static final Set<String> IDEMPOTENT_PATHS = Set.of(
            "/api/v1/transfers", "/api/v1/payments",
            "/api/v1/admin/wallets/{id}/test-funding", "/api/v1/admin/transactions/{id}/reversal");

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/register", "/api/v1/auth/register/merchant", "/api/v1/auth/login", "/api/v1/health");

    @Bean
    public OpenAPI ledgerOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Financial Ledger API (Team QUASAR)").version("0.3.0")
                        .description("Double-entry digital wallet and ledger. Money is always an integer number of minor units "
                                + "(amountMinor, paise for INR). Errors use the ErrorResponse schema; codes are listed per operation."));
    }

    /**
     * Runs after springdoc has built the document. Components are added here (not in the OpenAPI bean) because
     * springdoc rebuilds the components section after the bean is created (DEF-003: ErrorResponse was a dangling $ref).
     */
    @Bean
    public OpenApiCustomizer standardResponsesCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents() == null ? new Components() : openApi.getComponents();
            openApi.setComponents(components);
            components.addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                    .bearerFormat("JWT"));
            Map<String, Schema> error = ModelConverters.getInstance().read(com.quasar.ledger.common.api.ErrorResponse.class);
            error.forEach(components::addSchemas);
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, op) -> describe(path, op)));
        };
    }

    /** Adds security, the standard error responses and, on starred endpoints, the Idempotency-Key header. */
    private static void describe(String path, Operation op) {
        boolean isPublic = PUBLIC_PATHS.contains(path);
        if (!isPublic) {
            op.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        }
        ApiResponses responses = op.getResponses() == null ? new ApiResponses() : op.getResponses();
        addError(responses, "400", "VALIDATION_ERROR (and IDEMPOTENCY_KEY_REQUIRED on starred endpoints)");
        if (!isPublic) {
            addError(responses, "401", "UNAUTHENTICATED");
            addError(responses, "403", "FORBIDDEN");
            addError(responses, "404", "NOT_FOUND (missing or not yours)");
        }
        addError(responses, "500", "INTERNAL_ERROR - rolled back, no partial effect");
        if (IDEMPOTENT_PATHS.contains(path)) {
            addIdempotencyHeader(op);
            // These endpoints answer 201 (ResponseEntity hides that from springdoc, which guessed 200) - DEF-003.
            ApiResponse ok = responses.remove("200");
            if (ok != null) {
                responses.addApiResponse("201", ok.description("Created. A retry with the same Idempotency-Key returns the "
                        + "stored 201 or 422 unchanged, with header Idempotent-Replayed: true."));
            }
            addError(responses, "409", "IDEMPOTENCY_KEY_REUSED, ALREADY_REVERSED");
            addError(responses, "422", "INSUFFICIENT_FUNDS, WALLET_NOT_ACTIVE, CURRENCY_MISMATCH, SAME_WALLET, "
                    + "INVALID_DESTINATION, NOT_REVERSIBLE");
            addError(responses, "503", "SERVICE_BUSY - write lock timeout; retry with the same Idempotency-Key "
                    + "(Retry-After header)");
        }
        op.setResponses(responses);
    }

    /** Documents the header once (DEF-003: it was listed twice), as required, with its format. */
    private static void addIdempotencyHeader(Operation op) {
        if (op.getParameters() != null) {
            op.getParameters().removeIf(p -> "Idempotency-Key".equals(p.getName()));
        }
        op.addParametersItem(new HeaderParameter().name("Idempotency-Key").required(true)
                .description("8-64 characters of letters, digits, '-' and '_'. Reuse it only when retrying the same request.")
                .schema(new StringSchema().pattern("^[A-Za-z0-9_-]{8,64}$")));
    }

    private static void addError(ApiResponses responses, String status, String description) {
        if (responses.containsKey(status)) {
            return;
        }
        responses.addApiResponse(status, new ApiResponse().description(description).content(new Content()
                .addMediaType("application/json", new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
    }
}
