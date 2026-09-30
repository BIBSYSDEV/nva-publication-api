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
| `ResourceTable` | Creates `nva-resources` and all 4 GSIs if missing, against whatever endpoint the client is configured for |
| `HandlerContainer` | Type-based DI: `register(Class, instance)` + `create(Class)` picks the constructor with the most matching parameter types. Override via `registerFactory` for edge cases. |
| `AuthorizerContextProvider` | Populates `requestContext.authorizer` — pluggable |
| `TestHeaderAuthorizerProvider` | Reads `X-Adapter-Authorizer` JSON header (local/dev only) |
| `MockIntegrations` | Second Javalin instance stubbing Cognito token + Customer API |

## Running locally

Start the backing services first — the adapter no longer embeds a database:

```bash
docker compose -f publication-adapter-rest/compose.yaml up -d
```

Then, from the repo root:

```bash
AWS_ENDPOINT_URL_DYNAMODB="http://localhost:8000" \
AWS_ACCESS_KEY_ID="local" \
AWS_SECRET_ACCESS_KEY="local" \
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
- `8000` — DynamoDB-compatible database from `compose.yaml`

The adapter creates `nva-resources` and its four GSIs on startup if they are
missing, so there is no separate bootstrap step. The container runs in-memory,
so state is lost when it stops.

`AWS_ENDPOINT_URL_DYNAMODB` is the per-service form on purpose. The global
`AWS_ENDPOINT_URL` would redirect *every* AWS client at once, which becomes wrong
as soon as a second service is added.

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
   `buildContainer()`:
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
- **No S3.** Intentional — the file-download/upload handlers need MinIO once
  that scope expands. See "Target: no AWS dependency" below.
- **Cognito token validation now runs.** `ApiGatewayProxyRequestBuilder` omits
  `requestContext.authorizer` entirely when there is no authorizer context, so
  `RestRequestHandler.validateAuthorization` performs its JWKS check and
  `RequestInfo` reads claims from the token. Requests carrying
  `X-Adapter-Authorizer` still bypass that, which is the point of the local
  harness — but an unverifiable `Authorization: Bearer` is now rejected instead
  of being treated as gateway-authorized.

## Roadmap

### Short term — broaden the REST surface
1. ~~**Integration test first.**~~ Done — `AdapterApplicationTest` boots the
   adapter on a random port and drives POST/GET over HTTP. Every handler wired
   from here on should get a case in it. Note that GET needs an explicit
   `Accept: application/json`; without it content negotiation picks a text type
   and the handler answers `303` with a landing-page `Location`.
2. ~~**Register the missing collaborator types.**~~ Done for everything that
   doesn't need S3 — 13 of 29 operations now route. It took no factories at all,
   only types registered in `buildContainer()` plus two env vars
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
5. **Retire `TestHeaderAuthorizerProvider` for real deployments.** The JWKS
   validation itself is already working — see "JWT came for free" below. What is
   left is ensuring the `X-Adapter-Authorizer` bypass cannot be reached in a
   deployed environment, either by not registering the provider outside local
   runs or by dropping the class from the production path entirely.
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
Detailed in "Target: no AWS dependency" below; summarised here to keep the
roadmap readable.

10. ~~**Replace `DynamoDBEmbedded` with ScyllaDB Alternator.**~~ Done in part and
    blocked in part. `DynamoDBEmbedded` is out of the production path — the
    adapter now builds a `DynamoDbClient` from configuration and creates the
    table on startup, and embedded DynamoDB is a test-only dependency. Which
    server it points at is unresolved: Alternator rejects `TransactWriteItems`.
    See "The blocker: transactions" below.
11. **Replace the remaining fakes with real services:** MinIO for S3, Keycloak
    for Cognito, Vault for Secrets Manager. The Customer API stub stays a stub,
    but moves behind a URL so the real service can replace it by configuration.
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
- **Migrating off the DynamoDB data model.** Still out of scope for this module,
  but no longer optional if no-AWS is firm: the transaction blocker below means
  the data model and the cloud dependency cannot be separated.
- **DynamoDB Streams → EventBridge fanout.** The streams feed the event handlers,
  and Alternator's stream support differs from DynamoDB's. Needs CDC (Debezium or
  similar) or a deliberate alternative. Decide before the event-handler adapter
  is built — it is the hardest remaining unknown.

## Target: no AWS dependency

The goal is production-shaped code wherever we can manage it, and where we
cannot, hacks that could survive in production. Combined with a hard constraint
of **no runtime dependency on AWS**, that settles several design questions.

### One seam, not two

The tempting shortcut is LocalStack: one container emulating every AWS service.
It is the wrong tool here. LocalStack exists to emulate AWS for testing, is not
meant to be run in production, and would give us a local environment that
deliberately does not resemble what we deploy. Every bug it hides is a bug we
find in production instead.

The alternative is better on every axis that matters: pick open source services
that speak the same wire protocols, and run **the same image locally and in
Platon**. The local stack stops being a simulation and becomes the deployment.

| Needed | Runs in Platon, no AWS |
|---|---|
| DynamoDB | ExtendDB over PostgreSQL — tested and working. Alternator was tested and rejected |
| S3 | MinIO — S3 wire protocol |
| Cognito | Keycloak — real OIDC, real login page |
| Secrets Manager | Vault, which Sikt already runs for Platon |
| EventBridge | Kafka or NATS (shared with the event-handler work, item 12) |
| API Gateway | HAProxy ingress plus what this module does itself |
| Lambda | this adapter, in a plain Deployment |

### The blocker: transactions

**ScyllaDB Alternator cannot back this service.** This was tested, not assumed:
the adapter was pointed at `scylladb/scylla:6.2` with `--alternator-port 8000`.
Table creation with all four GSIs succeeded, `GET /by-owner` returned 200 — and
creating a publication failed with

```
DynamoDbException: Unsupported operation TransactWriteItems
```

Alternator does not implement DynamoDB's transaction API, and this codebase is
built on it: `TransactWriteItems` appears **101 times** across `publication-commons`
main code — `Dao` and every DAO subclass, `ResourceService`, `UpdateResourceService`,
`CristinIdentifierCounterService`, and `ServiceWithTransactions`, whose whole
purpose is batching transactional writes. Pointing at `amazon/dynamodb-local`
instead made the identical request succeed, which confirms transactions are the
only difference.

### ExtendDB works — tested, not assumed

**ExtendDB 0.1.12 runs this service on PostgreSQL.** It is an Apache-2.0 project
managed by AWS (announced May 2026) implementing the DynamoDB wire protocol in
Rust over PostgreSQL 14+. The AWS SDK talks to it unchanged; the data lands in
Postgres. It was run against this adapter, with these results:

- **Table creation with all four GSIs** succeeded.
- **`TransactWriteItems` succeeded** — creating a publication, the exact call
  Alternator rejects, went through. So did `DELETE` (202).
- **GSI-backed queries worked**: `/by-owner`, `/{id}/tickets` and `/{id}/log`
  all returned 200.
- **Zero `DynamoDbException` in the whole run.** The only failures were
  `ForbiddenException` from `TicketResolver.validateUserPermissions` — business
  rules about access rights, unrelated to storage.
- Data verified in Postgres: the table and its four GSIs as separate relations,
  plus `stream_records` and `idempotency_tokens`.

Its documented gaps do not touch us. PartiQL (`ExecuteStatement`,
`BatchExecuteStatement`, `ExecuteTransaction`) returns `UnknownOperationException`
— this repo uses none of them, verified as zero occurrences. Local secondary
indexes are likewise unused. GSIs carry a configurable
`index_propagation_delay_ms` that can be set to zero for synchronous updates.
Streams are supported, which also bears on the Streams→EventBridge question the
event-handler work treats as its hardest unknown.

Run it with the `extenddb` profile in `compose.yaml`. Practical notes:

- **TLS is mandatory** and the certificate is self-signed. The Java SDK does not
  honour `AWS_CA_BUNDLE`, so the cert must go into a JVM truststore — copy the
  JDK's `cacerts`, `keytool -importcert` the cert from
  `/var/lib/extenddb/.extenddb/tls/cert.pem`, and point
  `javax.net.ssl.trustStore` at it.
- **ExtendDB enforces IAM.** A fresh user gets no DynamoDB access; create the
  user, an access key, and attach a policy with `extenddb manage`.
- Port 18443, HTTPS, region `us-east-1` by default.

**What this still does not prove.** The adapter exercises 13 REST operations;
it is not the `publication-commons` test suite. Before committing, point that
suite at ExtendDB — it covers transaction rollback, conditional writes and GSI
consistency far more thoroughly than a smoke test can. The
`index_propagation_delay_ms` default of 10 ms is worth setting to zero there,
since several tests read back immediately through a GSI.

That said, the tradeoff below has largely dissolved: the data model survives and
the cloud dependency does not.

### If ExtendDB does not hold up

Three options, and the choice is a real decision rather than an implementation
detail:

1. **Keep DynamoDB.** Everything else on this list can leave AWS; the database
   cannot. This contradicts a strict no-AWS goal but costs nothing else.
2. **Move to PostgreSQL.** Real transactions, and the single-table design plus
   four GSIs get redesigned into something relational. A large project with its
   own risk, but it is the only option that satisfies both no-AWS and the
   atomicity the code currently relies on.
3. **Drop transactions and run on Alternator.** Cheapest to say, worst to live
   with: the atomic writes exist to keep resources, tickets, files and
   identifier entries consistent. Giving that up trades a migration project for
   a class of data-integrity bugs.

If ExtendDB fails the fuller test suite, (2) is the honest answer when no-AWS is
firm, and (1) is defensible if the real goal is leaving Lambda and API Gateway
rather than leaving AWS entirely. What should not happen is drifting into (3)
because it looks like less work.

`compose.yaml` still defaults to `amazon/dynamodb-local` — free but proprietary
and test-only — because it needs no TLS or IAM setup and keeps the quick path
quick. The `extenddb` profile is the one that matches the deployment target, and
should become the default once the `publication-commons` suite has run against
it. Platon provides managed PostgreSQL, so deploying this means running the
ExtendDB container against that rather than operating a database ourselves.

### What "no AWS" does and does not mean here

Worth being precise, because the strict reading is a far bigger project.

The AWS **SDK stays** as a client library. MinIO speaks S3 over the wire, so
`S3Client` keeps working against a server that has nothing to do with Amazon.
That is a protocol dependency, not a vendor one, and it is what makes the file
handling tractable.

The same argument was meant to cover DynamoDB, and it does not — see the
transaction blocker above. That is the difference between the two: S3's protocol
has a production-grade open source implementation, DynamoDB's does not, at least
not one covering the parts this code uses.

One genuine leftover: `com.amazonaws:aws-lambda-java-core` supplies the `Context`
interface that `handleRequest` takes, which is why `MockLambdaContext` exists.
That comes from the `nva-commons` handler signature and only goes away if those
signatures change.

### Configuration is the only difference between environments

Every client is pointed somewhere by configuration, never by a code path:

- AWS SDK v2 (2.54.9 here) reads `AWS_ENDPOINT_URL` and the per-service
  `AWS_ENDPOINT_URL_S3` / `_DYNAMODB` variants. Pointing at MinIO or Alternator
  is an env var, not a branch. Prefer the per-service variants — the global one
  redirects *every* service at once, which is rarely what you want.
- Credentials come from the standard provider chain, so Vault or Kubernetes
  secrets feed them as env vars without special handling.
- The OIDC issuer is already `COGNITO_AUTHORIZER_URLS`, which Keycloak satisfies
  unchanged.

That rules out a "local mode" flag in the code. It also means the fakes now in
`buildContainer` are not something to point elsewhere — they are something
to **delete**.

### The fakes, judged against that bar

| Today | Verdict |
|---|---|
| `DynamoDBEmbedded` | **Done** — gone from the production path, now a test-only dependency. The adapter builds a client from config and bootstraps the table |
| `fakeSecretsManagerClient()` — a `Proxy` returning canned credentials | Delete. Real client, Vault-provided secret |
| `noopEventBridgeClient()` — a `Proxy` swallowing events | Delete. Silently dropping events is not a behaviour to ship |
| `IdentityServiceClient.unauthorizedIdentityServiceClient()` | Replace with a configured client |
| `TestHeaderAuthorizerProvider` | **Must not exist in the production artifact** — see below |
| Customer API stub | Stays a stub, but moves out of process (see below) |

### The one that is dangerous, not just untidy

`TestHeaderAuthorizerProvider` turns an `X-Adapter-Authorizer` header into a
trusted authorizer context. It is compiled into the same artifact that would be
deployed, and nothing but the absence of that header stops it. Bearer token
validation now works (see below), so this is the remaining hole.

Configuration is not sufficient here — a misconfiguration would be an
authentication bypass. The harness classes belong in a separate source set or
module that the production build does not include, so that shipping the bypass
becomes a compile error rather than a deployment mistake.

### What genuinely cannot become real

The Customer API is an NVA service, not a standard one, so there is no open
source equivalent to run. It stays a stub — but it should move out of this
process into its own container behind a URL, so that pointing at the real
service later is a configuration change. A stub reachable over HTTP is a hack
that can live in production wiring; a stub compiled into the adapter is not.

### How it fits together

One `compose.yaml` runs the database, MinIO, Keycloak and the Customer API stub.
The same images back the integration tests through Testcontainers, which is
already in `nvaCatalog` (2.0.5), so the demo environment and the test environment
cannot drift apart. Keep the current in-process tests fast and untagged, and tag
the container-backed ones `@Tag("integrationTest")`, matching the convention in
the rest of this repo. Platon's runners have docker-in-docker, so CI can run
them.

### Sequencing

Order matters, because each step de-risks the next.

1. ~~**Get `DynamoDBEmbedded` out of the production path.**~~ Done, and it paid
   for itself immediately: putting a real server behind the endpoint is what
   surfaced the Alternator transaction blocker, on ground we already had tests
   for. Which server it points at is now an open decision, not an assumption.
2. **Vault-backed secrets and a real EventBridge-equivalent, deleting both
   `Proxy` fakes.** Small, and removes the two things that can never ship.
3. **MinIO and the nine file operations.** The mechanism is established by then,
   so this is about handler wiring rather than infrastructure.
4. **Keycloak.** Independent of 1–3 and can run in parallel. The JWKS validation
   it needs already works.
5. **Split the harness out of the production artifact.** Do this before anything
   is deployed anywhere reachable, not after.

### Known traps

- **Presigned URLs.** `CreatePresignedDownloadUrlHandler` takes an `S3Presigner`.
  Against MinIO this usually needs path-style access enabled, and the signed host
  must be the one the *client* can reach — not the in-cluster service name. This
  will not work by accident.
- **Bootstrapping tables and buckets.** Something has to create the table, its
  four GSIs and the buckets. It must be shared between `compose.yaml` and
  Testcontainers, or the two environments will differ in exactly the way this
  whole approach is meant to prevent.
- **Issuer matching.** The token issuer must equal `COGNITO_AUTHORIZER_URLS`
  exactly, and Keycloak's issuer embeds the realm name. `UrlJwkProvider` appends
  `/.well-known/jwks.json` and only keeps the scheme if the value already starts
  with `http`.
- **Cognito's `custom:` claims.** `custom:customerId`, `custom:accessRights` and
  the rest need Keycloak protocol mappers to come out under those exact names.
  Commit the realm export so this is reproducible rather than hand-configured.
- **Alternator is not DynamoDB.** Confirmed the hard way — see the transaction
  blocker above. The lesson generalises: verify any DynamoDB-compatible candidate
  against the existing `publication-commons` suite before planning around it.

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
- **The request size limit is stricter than production, not absent.** Javalin
  defaults `http.maxRequestSize` to 1 MB and answers `413 Content Too Large`
  (verified against a running adapter with a 3 MB body). API Gateway allows
  10 MB, so a payload that works in production is rejected here. Raise
  `maxRequestSize` to match rather than leaving it unbounded — the cap itself is
  what keeps a large POST from being buffered in heap by
  `ApiGatewayProxyRequestBuilder.addBody`.
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

### Closing those gaps on Platon

Assuming Platon is the target, since that is Sikt's PaaS. The conclusions differ
from what they would be on a self-managed cluster, so they are written down here
rather than rediscovered.

**A serverless framework is not the answer.** OpenFaaS, Knative and Nuclio all
speak plain HTTP in and HTTP out — none of them knows what `requestContext.authorizer`
or `pathParameters` are. `ApiGatewayProxyRequestBuilder`, `GatewayResponseWriter`
and `HandlerContainer` would survive unchanged and merely run inside a different
supervisor; the Javalin setup they would replace is the easy part. They also do
nothing about per-request handler construction, which follows from `nva-commons`
holding request state on the instance. Platon deploys plain Deployments anyway,
so this is moot here — but it is worth knowing it was considered and rejected on
its merits, not just on platform constraints.

**The ingress cannot absorb the API Gateway features.** This is the important
constraint. On a cluster where you control ingress, the natural move is to put
Kong or Envoy Gateway in front and let it do CORS, OpenAPI request validation,
rate limiting, JWT validation and payload limits — API Gateway's job, nearly 1:1,
with no change to adapter code. Platon runs HAProxy Ingress with a deliberately
conservative allowlist of seven annotations: `allow-list`, `auth-realm`,
`auth-secret`, `auth-type`, `cr-backend`, `path-rewrite`, `timeout-server`.
Notably, nginx's `proxy-body-size` was dropped in the HAProxy cut-over with no
equivalent. So:

| Gap | Where it has to live on Platon |
|---|---|
| CORS + `OPTIONS` preflight | the adapter — Javalin's CORS plugin |
| Payload size limit | the adapter — Javalin `http.maxRequestSize` |
| JWT validation | the adapter — but see below, `nva-commons` already does it |
| OpenAPI request validation | the adapter, or accepted as a known difference |
| Rate limiting | the adapter, or ask Platon |
| Request timeout | ingress — `haproxy.org/timeout-server`, plus an in-process deadline so the server actually stops working |
| IP allow-listing | ingress — `haproxy.org/allow-list` |

The pattern is that most of it lands back in this module. That is the single
biggest thing to know before estimating the work.

**Almost none of it needs a new library.** That surprised me, so it is worth
listing what is already available:

| Need | What provides it |
|---|---|
| CORS + preflight | Javalin, `bundledPlugins.enableCors(...)` |
| Payload limit | Javalin, `http.maxRequestSize` (already active at 1 MB) |
| Access logging | Javalin, `requestLogger.http(...)` |
| Rate limiting | Javalin, `NaiveRateLimit` — per pod and in-memory, so it bounds a single instance rather than the service |
| Graceful shutdown | Jetty stop timeout via `jetty.modifyServer`, plus a shutdown hook |
| Health / readiness | a couple of plain Javalin routes |
| JWT validation | `nva-commons` — see below |
| OpenAPI request validation | **new dependency**: `com.atlassian.oai:swagger-request-validator-javalin` |
| Prometheus metrics | **new dependency**: Micrometer + its Javalin plugin |

**JWT came for free — already done.** `RestRequestHandler` validates bearer
tokens against JWKS (`com.auth0:jwks-rsa`, which caches keys), and
`RequestInfo.fetchUserInfo` reads claims straight from the token when the request
is not gateway-authorized. Both paths used to be dead code, because
`ApiGatewayProxyRequestBuilder` always wrote a `requestContext.authorizer`
object — and an empty one counts, since `isGatewayAuthorized()` only checks that
the node exists and is an object.

The fix was to omit the key rather than write a JWT validator.
`ApiGatewayProxyRequestBuilderTest` pins both directions, because the failure
mode is silent: an empty object turns authentication **off** rather than on, and
nothing in the response distinguishes the two. What remains for production is
deciding whether `TestHeaderAuthorizerProvider` should be compiled in at all, or
swapped for one that never trusts a header.

**What Platon does give.** TLS termination, image scanning, review environments
per branch, and a deployment template that already has liveness and readiness
probes and resource limits. Two caveats on that template:

- Its probes point at `/`, which for this adapter is the **POST** create route.
  There is no `GET /`, so the probes need real endpoints — and readiness should
  fail while DynamoDB is unreachable while liveness should not.
- Its defaults are `500m` CPU and `512Mi` memory. The Lambda functions run at
  `1800` MB by default, and `NvaUpdatePublicationFunction` and
  `PublishPublicationFunction` — both wired in this adapter — run at `8192` MB.
  A pod serving several concurrent requests needs sizing from measurements, not
  from the template defaults.

**One alternative worth naming (not chosen).** The Lambda Runtime Interface Emulator (or
`aws-lambda-java-runtime-interface-client`) would run the handlers in the real
Lambda runtime inside a container: a genuine deadline behind
`getRemainingTimeInMillis()`, a real request id, and one invocation at a time per
instance — which is exactly the guarantee `nva-commons` is written against, so
the thread-safety problem disappears rather than being fixed. The cost is the
concurrency model: parallelism costs processes instead of threads, and you would
still have to build the proxy event yourself, since RIE accepts a payload rather
than an HTTP request. That is rebuilding Lambda on top of Kubernetes, which
gives up much of the reason to move.
