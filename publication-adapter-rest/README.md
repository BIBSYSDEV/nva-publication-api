# publication-adapter-rest

Proof-of-concept adapter that runs the existing `ApiGatewayHandler` subclasses from
`publication-rest` inside a plain JVM process — outside AWS Lambda. The handlers are
**not modified**; the adapter translates between an HTTP server (Javalin) and the API
Gateway proxy event / `GatewayResponse` format that `nva-commons` already understands.

Purpose: evaluate what it would take to move the publication API to a Kubernetes
deployment while keeping the existing handler code.

## How it works

```
HTTP request
   │
   ▼
Javalin  ──►  ApiGatewayProxyRequestBuilder  ──►  API Gateway Proxy JSON (InputStream)
                                                         │
                                                         ▼
                                           handler.handleRequest(in, out, mockCtx)
                                                         │
                                                         ▼
HTTP response ◄── GatewayResponseWriter ◄── GatewayResponse JSON (OutputStream)
```

Route registration is driven by `docs/openapi.yaml` via the `x-handler-class`
extension. The adapter looks up each operation's handler class at startup; a
`HandlerFactory` per class provides local wiring (embedded DynamoDB, fake Secrets
Manager, etc.).

## Key components

| File | Purpose |
|---|---|
| `AdapterApplication` | Main entry: parses OpenAPI, starts WireMock + Javalin + embedded DynamoDB |
| `ApiGatewayProxyRequestBuilder` | HTTP → API Gateway Proxy JSON |
| `GatewayResponseWriter` | `GatewayResponse` JSON → HTTP |
| `MockLambdaContext` | Minimal `com.amazonaws.services.lambda.runtime.Context` stub |
| `LocalDynamoDb` | Starts `DynamoDBEmbedded`, creates `nva-resources` table + all 4 GSIs |
| `HandlerContainer` | Type-based DI: `register(Class, instance)` + `create(Class)` picks the constructor with the most matching parameter types. Override via `registerFactory` for edge cases. |
| `AuthorizerContextProvider` | Populates `requestContext.authorizer` — pluggable |
| `TestHeaderAuthorizerProvider` | Reads `X-Adapter-Authorizer` JSON header (local/dev only) |
| `MockIntegrations` | WireMock server stubbing Cognito token + Customer API |

## Running locally

From the repo root:

```bash
OPENAPI_PATH=/absolute/path/to/docs/openapi.yaml \
ALLOWED_ORIGIN="*" \
AWS_REGION="eu-west-1" \
COGNITO_HOST="http://localhost:8090" \
ID_NAMESPACE="https://www.example.org/publication" \
BACKEND_CLIENT_SECRET_NAME="secret" \
TABLE_NAME="nva-resources" \
BACKEND_CLIENT_AUTH_URL="http://localhost:8090" \
EXTERNAL_USER_POOL_URI="http://localhost:8090/external" \
API_HOST="localhost" \
COGNITO_AUTHORIZER_URLS="http://localhost:3000" \
NVA_FRONTEND_DOMAIN="localhost" \
./gradlew :publication-adapter-rest:run
```

Ports:
- `8080` — adapter HTTP (override with `PORT`)
- `8090` — WireMock for Cognito + Customer API (override with `MOCK_PORT`)

Embedded DynamoDB runs in-process; state is lost when the JVM exits.

## Example requests

### GET — no authorization needed, hits embedded DynamoDB

```bash
curl http://localhost:8080/019db6fe6118-cb77b42d-4ca2-47cf-8f5c-b1624e76b4f3
# → 404 until something has been created
```

### POST — full authenticated flow

```bash
AUTHORIZER='{
  "custom:customerId":"http://localhost:8090/customer/550e8400-e29b-41d4-a716-446655440000",
  "custom:nvaUsername":"testuser@sikt.no",
  "custom:accessRights":"MANAGE_OWN_RESOURCES",
  "custom:cristinId":"https://api.cristin.no/person/12345",
  "custom:topOrgCristinId":"https://api.cristin.no/organization/7482.0.0.0",
  "custom:personAffiliation":"https://api.cristin.no/organization/7482.0.0.0"
}'

curl -X POST http://localhost:8080/ \
  -H 'Content-Type: application/json' \
  -H "X-Adapter-Authorizer: $AUTHORIZER" \
  -d '{}'
# → 201 Created with full PublicationResponse
```

The customer URI in the authorizer **must** point at the WireMock port
(`http://localhost:8090/customer/...`) so that `JavaHttpClientCustomerApiClient`
hits the stub instead of an external service.

## Adding a handler

1. Add `x-handler-class: <fqn>` to the operation in `docs/openapi.yaml`.
2. **If the handler only needs types already registered in `HandlerContainer`** —
   nothing else to do. The container picks the constructor with the most
   matching parameter types (which naturally skips `@JacocoGenerated` no-arg
   constructors), so the handler is instantiated automatically.
3. **If it needs a new collaborator** — register the type once in
   `buildLocalContainer()`:
   ```java
   .register(SomeNewClient.class, fakeSomeNewClient())
   ```
4. **Only for handlers that can't be satisfied by plain type-matching** (e.g.
   handlers that need per-instance configuration beyond what types describe),
   register an explicit factory:
   ```java
   container.registerFactory(WeirdHandler.class, c ->
       new WeirdHandler(c.lookup(ResourceService.class).orElseThrow(),
                        buildSomethingCustom()));
   ```

Currently registered types: `ResourceService`, `Environment`, `RawContentRetriever`,
`IdentityServiceClient`, `SecretsManagerClient`, `HttpClient`.

Operations without `x-handler-class` are skipped at startup with a `Skipping ...`
log line. If the container can't satisfy any constructor for a registered class,
the request fails with `No constructor of X could be satisfied by registered
services. Registered: [...]`.

## Design choices / known limitations

- **Not production code.** `IdentityServiceClient.unauthorizedIdentityServiceClient()`,
  a `Proxy`-based fake `SecretsManagerClient`, and `TestHeaderAuthorizerProvider`
  all take shortcuts that are acceptable for a local harness but not for a live
  deployment.
- **JaCoCo coverage verification is disabled** for this module — it's a harness,
  not shipping logic.
- **Only two handlers wired:** `FetchPublicationHandler` (GET) and
  `CreatePublicationHandler` (POST). The remaining ~20 REST operations need
  factories.
- **Jetty version alignment:** Javalin 6.7 (Jetty 11), WireMock 3.13.1 (Jetty 11),
  DynamoDBLocal (Jetty excluded). Upgrading any of these requires re-checking the
  classpath.
- **No S3.** Intentional — file-download/upload handlers will need either MinIO
  or LocalStack once that scope expands.
- **No Cognito token validation.** `RestRequestHandler` will still validate the
  bearer token signature against `COGNITO_AUTHORIZER_URLS` if one is present,
  but the PoC flow bypasses that by populating `requestContext.authorizer`
  directly.

## Roadmap

### Short term — broaden the REST surface
1. **Factory per remaining REST handler.** 22 operations total; 2 done. Most need
   the same three collaborators (`ResourceService`, a fake Secrets client, a
   `CustomerApiClient`), so a small shared helper should cover the bulk.
2. **Integration tests (JUnit)** that boot the adapter on a random port, seed
   embedded DynamoDB directly, and drive requests via an HTTP client. Restores
   some of the coverage lost to `jacocoTestCoverageVerification = false`.
3. **Drop explicit factories for handlers with zero local wiring.** A
   convention-based fallback that calls the no-arg constructor would work for
   read-only handlers once they stop reaching for Secrets/S3 at init time.

### Medium term — productionize the adapter
4. **`JwtAuthorizerProvider`** — decode and validate the `Authorization: Bearer`
   token against a JWKS endpoint, extract claims, populate `authorizer.claims`.
   Replaces `TestHeaderAuthorizerProvider` in any real deployment.
5. **`HeaderClaimsAuthorizerProvider`** — for K8s deployments where Envoy/Kong
   validates the JWT at the edge and injects `x-user-*` headers. Cheaper than
   re-validating in the adapter.
6. **Package as a container image.** Dockerfile + distroless JRE, a small
   `entrypoint.sh` that honours `PORT`/`OPENAPI_PATH`.
7. **Kubernetes manifests** (Deployment + Service + ConfigMap + HPA). One
   Deployment per logical pool: standard REST pod, and a larger pod for
   `UpdatePublicationHandler` (8192 MB Lambda today).
8. **Observability.** Structured logs (already flowing through log4j2), Prometheus
   `/metrics` endpoint via Micrometer, OpenTelemetry traces spanning adapter →
   handler → DynamoDB.

### Long term — replace the mocks with real services
9. **Swap embedded DynamoDB for real DynamoDB (or ScyllaDB Alternator).** The
   handler code is unchanged — only the client factory needs an endpoint override
   and credentials provider.
10. **Swap WireMock for the real Customer API / Cognito.** Same mechanism: env
    vars. The PoC stubs are there to make local smoke-testing possible without
    a dev environment.
11. **Event handlers** — `publication-adapter-events` as a sibling module.
    Applies the same pattern (`EventHandler.handleRequest(in, out, ctx)`) with
    a Kafka/NATS consumer feeding `AwsEventBridgeEvent` JSON to handlers. This
    is the bigger piece of work and covers ~40 of the project's handlers.
12. **Retire `template.yaml` as a deployment artifact** once everything runs in
    K8s. Keep it only as long as dual deployment (AWS Lambda + K8s) is needed.

### Out of scope for now
- **DynamoDB single-table migration.** If a future decision is "leave AWS
  entirely," the single-table design plus 4 GSIs is the hardest thing to move.
  The adapter doesn't make that decision any easier or harder — it's orthogonal.
- **DynamoDB Streams → EventBridge fanout.** Needs CDC (Debezium or similar) or
  a keep-DynamoDB strategy. Decide before the event-handler adapter is built.
