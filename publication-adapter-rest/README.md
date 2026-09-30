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
| `AdapterApplication` | Main entry: parses OpenAPI, starts mock integrations + Javalin + embedded DynamoDB |
| `ApiGatewayProxyRequestBuilder` | HTTP → API Gateway Proxy JSON |
| `GatewayResponseWriter` | `GatewayResponse` JSON → HTTP |
| `MockLambdaContext` | Minimal `com.amazonaws.services.lambda.runtime.Context` stub |
| `LocalDynamoDb` | Starts `DynamoDBEmbedded`, creates `nva-resources` table + all 4 GSIs |
| `HandlerContainer` | Type-based DI: `register(Class, instance)` + `create(Class)` picks the constructor with the most matching parameter types. Override via `registerFactory` for edge cases. |
| `AuthorizerContextProvider` | Populates `requestContext.authorizer` — pluggable |
| `TestHeaderAuthorizerProvider` | Reads `X-Adapter-Authorizer` JSON header (local/dev only) |
| `MockIntegrations` | Second Javalin instance stubbing Cognito token + Customer API |

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
CUSTOM_DOMAIN_BASE_PATH="publication" \
NVA_EVENT_BUS_NAME="local-event-bus" \
./gradlew :publication-adapter-rest:run
```

Ports:
- `8080` — adapter HTTP (override with `PORT`)
- `8090` — mock integrations for Cognito + Customer API (override with `MOCK_PORT`)

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

The customer URI in the authorizer **must** point at the mock port
(`http://localhost:8090/customer/...`) so that `JavaHttpClientCustomerApiClient`
hits the stub instead of an external service.

## Adding a handler

1. Add `x-handler-class: <fqn>` to the operation in `docs/openapi.yaml`. The file
   is read with `swagger-core`'s YAML mapper, which does not resolve `$ref` —
   only `paths`, HTTP methods and extensions are used for routing.
2. **If the handler only needs types already registered in `HandlerContainer`** —
   nothing else to do. The container picks the constructor with the most
   matching parameter types, so the handler is instantiated automatically.
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

Currently registered types: `ResourceService`, `TicketService`, `MessageService`,
`PublishingService`, `TicketResolver`, `Environment`, `RawContentRetriever`,
`IdentityServiceClient`, `SecretsManagerClient`, `HttpClient`, `EventBridgeClient`,
`DataCiteDoiClient` / `DoiClient`.

Matching is on **exact** parameter type, not assignability — a handler declaring
an interface needs that interface registered, which is why `DoiClient` and
`DataCiteDoiClient` both point at the same instance.

A handler whose collaborators aren't all registered fails loudly. It deliberately
does **not** fall back to the no-arg constructor, since those call
`ResourceService.defaultService()` and would quietly talk to real AWS.

Operations without `x-handler-class` are skipped at startup with a `Skipping ...`
log line. If the container can't satisfy any constructor for a registered class,
the request fails with `No constructor of X could be satisfied by registered
services. Registered: [...]`.

## Design choices / known limitations

- **Not production code.** `IdentityServiceClient.unauthorizedIdentityServiceClient()`,
  a `Proxy`-based fake `SecretsManagerClient`, and `TestHeaderAuthorizerProvider`
  all take shortcuts that are acceptable for a local harness but not for a live
  deployment.
- **No code-quality gates.** The module deliberately skips the
  `nva.publication.api.java-conventions` plugin, so Checkstyle, PMD and JaCoCo
  don't run here — it's a harness, not shipping logic. JUnit is wired up
  directly in `build.gradle` instead, along with the env vars the handlers read
  at construction time.
- **13 of 29 operations wired.** Publication CRUD, publish, by-owner, the four
  ticket operations, log and context. `AdapterApplicationTest` asserts that every
  `x-handler-class` in `docs/openapi.yaml` can actually be constructed.
- **One handler instance per request — by necessity, not by accident.** The
  `nva-commons` handler hierarchy keeps per-request state on the instance
  (`outputStream`, `allowedOrigin`, `isBase64Encoded`), so instances must not be
  shared across threads. The constructor lookup is cached; the instance is not.
- **Jetty version alignment:** Javalin 6.7 (Jetty 11) and DynamoDBLocal (Jetty
  excluded). The Cognito/Customer stubs run on a second Javalin instance rather
  than WireMock precisely to keep one Jetty on the classpath — `nvaCatalog`'s
  WireMock 4 pulls a newer Jetty that breaks Javalin's websocket-core with a
  `NoSuchMethodError` at startup.
- **No S3.** Intentional — file-download/upload handlers will need either MinIO
  or LocalStack once that scope expands.
- **No Cognito token validation.** `RestRequestHandler` will still validate the
  bearer token signature against `COGNITO_AUTHORIZER_URLS` if one is present,
  but the PoC flow bypasses that by populating `requestContext.authorizer`
  directly.

## Roadmap

### Short term — broaden the REST surface
1. ~~**Integration test first.**~~ Done — `AdapterApplicationTest` boots the
   adapter on a random port and drives POST/GET over HTTP. Every handler wired
   from here on should get a case in it. Note that GET needs an explicit
   `Accept: application/json`; without it content negotiation picks a text type
   and the handler answers `303` with a landing-page `Location`.
2. ~~**Register the missing collaborator types.**~~ Done for everything that
   doesn't need S3 — 13 of 29 operations now route. It took no factories at all,
   only types registered in `buildLocalContainer()` plus two env vars
   (`CUSTOM_DOMAIN_BASE_PATH`, `NVA_EVENT_BUS_NAME`). What remains:
   - **7 file-upload + 2 download operations** need S3 (see "No S3" above).
   - **2 message operations** live in a module this one doesn't depend on.
   - **4 import-candidate operations** construct fine but would read the
     resources table; they need their own table before being switched on.
3. ~~**Fail the build on drift.**~~ Done — `shouldNotLeaveNewOperationsUnwiredWithoutSayingSo`
   compares the operations lacking `x-handler-class` against
   `KNOWN_UNWIRED_OPERATIONS`, which carries a reason per entry. A new operation
   added to `docs/openapi.yaml` now fails the build instead of silently logging
   `Skipping`. Its sibling `shouldInstantiateEveryHandlerDeclaredInOpenApi`
   guards the other direction.

With that the short-term list is done: the REST surface is as broad as it can
get without S3, and both directions of drift now fail the build.

### Medium term — productionize the adapter
4. ~~**Cache handler instances.**~~ Investigated and deliberately **not** done —
   handler instances cannot be shared, and the reason is in `nva-commons`, not
   in our handlers:
   - `RestRequestHandler.init()` stores the request's `OutputStream` on the
     instance (`RestRequestHandler.java:283`), along with `allowedOrigin`.
   - `ApiGatewayHandler` adds `isBase64Encoded` and
     `additionalSuccessHeadersSupplier` as instance fields.
   - Individual handlers add their own, e.g. `FetchPublicationHandler.statusCode`,
     reset "on each invocation" (line 121).

   Under Lambda one instance serves one request at a time, so this is sound
   there. Sharing an instance across threads would let concurrent requests write
   into each other's response. Caching the *instances* therefore requires
   changing `nva-commons` — a much larger decision than this module.

   What was done instead: `HandlerContainer` caches the resolved `Constructor`
   per handler class, so the reflective lookup happens once rather than per
   request, while each request still gets its own instance.
   `shouldHandleConcurrentRequestsWithoutMixingUpResponses` drives 16 concurrent
   creates to keep that honest.
5. **`JwtAuthorizerProvider`** — decode and validate the `Authorization: Bearer`
   token against a JWKS endpoint, extract claims, populate `authorizer.claims`.
   Replaces `TestHeaderAuthorizerProvider` in any real deployment. This is the
   default; treat it as the one to build.
6. **`HeaderClaimsAuthorizerProvider`** — only for deployments where Envoy/Kong
   validates the JWT at the edge. It trusts injected `x-user-*` headers
   unconditionally, so it is safe *only* if the pod is unreachable from outside
   the mesh. A network-policy decision, not merely a cheaper alternative to (5).
7. **Package as a container image.** Dockerfile + distroless JRE, a small
   `entrypoint.sh` that honours `PORT`/`OPENAPI_PATH`.
8. **Kubernetes manifests** (Deployment + Service + ConfigMap + HPA). One
   Deployment per logical pool: standard REST pod, and a larger pod for
   `UpdatePublicationHandler` (8192 MB Lambda today).
9. **Observability.** Structured logs (already flowing through log4j2), Prometheus
   `/metrics` endpoint via Micrometer, OpenTelemetry traces spanning adapter →
   handler → DynamoDB.

### Long term — replace the mocks with real services
10. **Swap embedded DynamoDB for real DynamoDB (or ScyllaDB Alternator).** The
    handler code is unchanged — only the client factory needs an endpoint override
    and credentials provider.
11. **Swap the stubs for the real Customer API / Cognito.** Same mechanism: env
    vars. The PoC stubs are there to make local smoke-testing possible without
    a dev environment.
12. **Event handlers** — `publication-adapter-events` as a sibling module.
    Applies the same pattern (`EventHandler.handleRequest(in, out, ctx)`) with
    a Kafka/NATS consumer feeding `AwsEventBridgeEvent` JSON to handlers. Covers
    the 21 `EventHandler` subclasses in the project. Bigger than the REST side,
    mostly because delivery semantics (retries, DLQ, ordering) have no direct
    Kafka/NATS equivalent to EventBridge + SQS.
13. **Split `template.yaml` rather than retiring it.** It is not only deployment:
    it also defines API Gateway authorizers, EventBridge rules, DLQs and alarms.
    Decide per section what moves to K8s manifests and what has to stay in AWS —
    and expect some of it (68 function definitions today) to stay for as long as
    dual deployment lasts.

### Out of scope for now
- **DynamoDB single-table migration.** If a future decision is "leave AWS
  entirely," the single-table design plus 4 GSIs is the hardest thing to move.
  The adapter doesn't make that decision any easier or harder — it's orthogonal.
- **DynamoDB Streams → EventBridge fanout.** Needs CDC (Debezium or similar) or
  a keep-DynamoDB strategy. Decide before the event-handler adapter is built.

## If this were to run in production

Everything below is fine to ignore for a PoC and **not** fine to ignore for real
traffic. The roadmap above is about breadth — how much of the API the adapter
covers. This section is about depth: the places where the adapter behaves
differently from API Gateway + Lambda, and where that difference would be felt.

### The adapter silently changes request semantics

These are the ones that worry me most, because nothing fails loudly — responses
are simply subtly different from production.

- **`getRemainingTimeInMillis()` returns a hardcoded 30 s**
  (`MockLambdaContext.java`), while the real functions have `Timeout: 20`
  (`template.yaml` Globals). Any handler that budgets work against the remaining
  time believes it has more room than it does, and there is no request timeout in
  the adapter to catch it. Needs a real deadline, propagated from a server-side
  timeout.
- **No request size limit.** `ctx.body()` reads the whole body into a `String`
  (`ApiGatewayProxyRequestBuilder.addBody`). API Gateway caps payloads at 10 MB
  and rejects larger ones before any code runs; here a large POST is simply
  buffered in heap. This is a denial-of-service shape, not a correctness bug.
- **Binary bodies are corrupted.** `isBase64Encoded` is hardcoded `false` on the
  way in, and the body is treated as text. Responses handle base64 correctly
  (`GatewayResponseWriter.writeBody`), so this is an inbound-only gap — it will
  surface the moment the file-upload handlers are wired.
- **Only single-value headers.** Query parameters get both
  `queryStringParameters` and `multiValueQueryStringParameters`, but headers only
  get the single-value map. A repeated header reaches the handler as one value.
- **`requestContext` carries only `authorizer`.** No `requestId`,
  `identity.sourceIp`, `stage`, `domainName` or `requestTimeEpoch`. Nothing in
  this repo's Java code reads them today (checked), so this is latent rather than
  broken — but `RequestInfo.getRequestContextParameter` throws rather than
  returning empty, so a future handler reaching for one fails at runtime instead
  of at startup.
- **Error responses bypass the NVA format.** If anything throws before
  `GatewayResponseWriter.write` runs, the client gets Javalin's default error page
  rather than the `application/problem+json` body API Gateway would produce.
  Clients that parse the problem format would see something they don't recognise.

### What API Gateway does today that the adapter does not

Worth an explicit decision per item — some belong in an ingress or service mesh
rather than in this process.

- **CORS, including `OPTIONS` preflight.** Configured globally in
  `template.yaml` (`Globals.Api.Cors`) and generated by API Gateway; the adapter
  registers no `OPTIONS` routes at all, so browser clients would break.
- **Access logging.** `NvaPublicationApi.AccessLogSetting` logs requestId,
  sourceIp, latency, status and userAgent per request. The adapter logs a
  registration line at startup and nothing per request.
- **Throttling and WAF.** Not in `template.yaml` today, so presumably handled at
  the account or CloudFront level — worth confirming where, because that layer
  does not move with the deployment.
- **Request validation against the OpenAPI schema.** The adapter reads
  `openapi.yaml` only for routing and `x-handler-class`; `$ref`s are not even
  resolved. Malformed bodies now reach handler code that previously never saw
  them.
- **Authorizer result caching.** API Gateway caches authorizer responses; any
  replacement for `TestHeaderAuthorizerProvider` should cache JWKS lookups
  deliberately rather than validating per request.

### Operational basics the adapter has none of

- **Health and readiness endpoints.** Kubernetes needs both, and they must differ:
  readiness should fail while DynamoDB is unreachable, liveness should not.
- **Graceful shutdown.** `javalin.stop()` is never wired to SIGTERM, so a rolling
  deploy drops in-flight requests. Needs a shutdown hook plus a `preStop` delay
  long enough for endpoint deregistration to propagate.
- **DynamoDB client tuning.** One shared `DynamoDbClient` with default settings.
  Under Lambda, concurrency was bounded by the number of function instances; in a
  pod it is bounded by `maxConcurrency` and the connection pool, which nobody has
  chosen yet.
- **JVM warmup.** Lambda's cold start becomes JIT warmup after a deploy. Worth
  measuring before setting HPA thresholds, or the first pod in a scale-up serves
  slow requests and triggers more scaling.

### Performance, once it matters

- **Per-request handler construction stays.** The constructor lookup is cached
  (see roadmap item 4), but each request still builds a handler and its object
  graph. That is a `nva-commons` constraint, not a choice. If profiling shows it
  matters, the fix is making the handler hierarchy stateless — a change with
  reach far beyond this module.
- **Nothing here has been profiled.** The concurrency test proves correctness
  under 16 parallel requests, not throughput. Any capacity claim needs real
  measurements first.
