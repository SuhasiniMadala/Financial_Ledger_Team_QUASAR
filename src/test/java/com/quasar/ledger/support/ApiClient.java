package com.quasar.ledger.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal HTTP client for end-to-end API tests (NFR-TEST-03). Talks to the real embedded server over
 * HTTP, so tests see exactly what Postman, the skills or the frontend would see.
 */
public class ApiClient {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;
    private final ObjectMapper json;

    public ApiClient(String baseUrl, ObjectMapper json) {
        this.baseUrl = baseUrl;
        this.json = json;
    }

    /** An HTTP response with the parsed JSON body (or null when the body is not JSON). */
    public record Response(int status, HttpResponse<String> raw, JsonNode body) {

        public String header(String name) {
            return raw.headers().firstValue(name).orElse(null);
        }

        public String text(String field) {
            JsonNode n = body == null ? null : body.get(field);
            return n == null || n.isNull() ? null : n.asText();
        }

        public long number(String field) {
            return body.get(field).asLong();
        }

        public String code() {
            return text("code");
        }

        @Override
        public String toString() {
            return status + " " + raw.body();
        }
    }

    public Response get(String path, String token) {
        return send("GET", path, null, token, Map.of());
    }

    public Response post(String path, Object body, String token) {
        return send("POST", path, body, token, Map.of());
    }

    public Response postIdempotent(String path, Object body, String token, String idempotencyKey) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (idempotencyKey != null) {
            headers.put("Idempotency-Key", idempotencyKey);
        }
        return send("POST", path, body, token, headers);
    }

    /** Sends a raw string body exactly as given (for malformed-JSON tests). */
    public Response postRaw(String path, String rawBody, String token) {
        return send("POST", path, new RawBody(rawBody), token, Map.of());
    }

    private record RawBody(String text) {
    }

    public Response send(String method, String path, Object body, String token, Map<String, String> headers) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(60));
            String payload = body == null ? null
                    : body instanceof RawBody raw ? raw.text() : json.writeValueAsString(body);
            b.method(method, payload == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(payload));
            if (payload != null) {
                b.header("Content-Type", "application/json");
            }
            if (token != null) {
                b.header("Authorization", "Bearer " + token);
            }
            headers.forEach(b::header);
            HttpResponse<String> response = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode parsed = null;
            String text = response.body();
            if (text != null && !text.isBlank() && (text.startsWith("{") || text.startsWith("["))) {
                parsed = json.readTree(text);
            }
            return new Response(response.statusCode(), response, parsed);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
