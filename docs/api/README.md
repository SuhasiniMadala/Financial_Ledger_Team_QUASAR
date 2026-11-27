# API Documentation (SE-7, NFR-MAIN-04)

| File | What it is |
|---|---|
| `openapi.json` | OpenAPI 3 specification of all 26 paths / 27 operations of SRS Table 3-1, with the Idempotency-Key header, the shared `ErrorResponse` schema and the error codes per operation. Generated from the running code. |
| `financial-ledger.postman_collection.json` | Postman collection: a complete happy path plus negative and idempotency cases (SRS 2.6). |

Interactive docs: start the server and open `http://localhost:8080/swagger-ui.html`.

## Refresh `openapi.json` after an API change

```powershell
mvn test -Dtest=OpenApiDocumentationTest -Djacoco.skip=true
Copy-Item target/openapi/openapi.json docs/api/openapi.json
```

`OpenApiDocumentationTest` fails if any Table 3-1 endpoint is missing, if a starred endpoint lacks the Idempotency-Key header or documents 200 instead of 201, or if a `$ref` points to a missing schema (DEF-003).

## Using the Postman collection

1. Postman -> Import -> choose `financial-ledger.postman_collection.json`.
2. Start a local server with an admin configured (see `brain/skills/simulate-transfer/SKILL.md`, "Before you run it").
3. In the collection's *Variables* tab set `baseUrl` (default `http://localhost:8080`), `adminEmail` and `adminPassword` (current value only - never save a real password into an exported file).
4. Run the requests in order (Collection Runner). Each request has tests that check the status code; tokens, wallet IDs and keys are stored in collection variables automatically.

The collection was checked to be valid JSON; it was not executed inside Postman during Stage 3 because the same flows are executed automatically by the API tests and by `simulate-transfer`.
