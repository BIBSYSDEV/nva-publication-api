package no.unit.nva.publication.adapter;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.external.services.ChannelClaimClient;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public final class AdapterApplication {

    private static final Logger logger = LoggerFactory.getLogger(AdapterApplication.class);
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_OPENAPI_PATH = "../docs/openapi.yaml";
    private static final String HANDLER_CLASS_EXTENSION = "x-handler-class";
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.dtoObjectMapper;
    private static final String FAKE_CREDENTIALS_JSON =
        "{\"id\":\"fake-client-id\",\"secret\":\"fake-client-secret\"}";

    private final HandlerContainer container;
    private final OpenAPI openApi;
    private final ApiGatewayProxyRequestBuilder requestBuilder;
    private final GatewayResponseWriter responseWriter;

    public AdapterApplication(HandlerContainer container, OpenAPI openApi) {
        this.container = container;
        this.openApi = openApi;
        this.requestBuilder = new ApiGatewayProxyRequestBuilder(
            OBJECT_MAPPER, new TestHeaderAuthorizerProvider(OBJECT_MAPPER));
        this.responseWriter = new GatewayResponseWriter(OBJECT_MAPPER);
    }

    public static void main(String[] args) {
        var openApiPath = System.getenv().getOrDefault("OPENAPI_PATH", DEFAULT_OPENAPI_PATH);
        var port = Integer.parseInt(System.getenv().getOrDefault("PORT", String.valueOf(DEFAULT_PORT)));
        var mockPort = Integer.parseInt(
            System.getenv().getOrDefault("MOCK_PORT", String.valueOf(MockIntegrations.DEFAULT_PORT)));
        var openApi = new OpenAPIV3Parser().read(openApiPath);
        if (openApi == null) {
            throw new IllegalStateException("Failed to parse OpenAPI at " + openApiPath);
        }
        MockIntegrations.start(mockPort);
        var container = buildLocalContainer();
        var app = new AdapterApplication(container, openApi);
        app.start(port);
    }

    public Javalin start(int port) {
        var javalin = Javalin.create();
        registerRoutes(javalin);
        javalin.start(port);
        logger.info("Adapter listening on port {}", port);
        return javalin;
    }

    private void registerRoutes(Javalin javalin) {
        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, op) -> {
            var handlerClass = resolveHandlerClass(op);
            if (handlerClass.isEmpty()) {
                logger.info("Skipping {} {} (no x-handler-class)", method, path);
                return;
            }
            var javalinPath = toJavalinPath(path);
            var handlerType = toHandlerType(method);
            javalin.addHttpHandler(handlerType, javalinPath, ctx -> invoke(handlerClass.get(), ctx));
            logger.info("Registered {} {} -> {}", method, javalinPath, handlerClass.get().getName());
        }));
    }

    private void invoke(Class<?> handlerClass, io.javalin.http.Context ctx) throws Exception {
        var handler = container.create(handlerClass);
        var proxyJson = requestBuilder.build(ctx, ctx.pathParamMap());
        try (var in = new ByteArrayInputStream(proxyJson.getBytes(StandardCharsets.UTF_8));
             var out = new ByteArrayOutputStream()) {
            handler.handleRequest(in, out, new MockLambdaContext());
            var responseJson = out.toString(StandardCharsets.UTF_8);
            responseWriter.write(ctx, responseJson);
        }
    }

    private static Optional<Class<?>> resolveHandlerClass(Operation operation) {
        if (operation.getExtensions() == null) {
            return Optional.empty();
        }
        Object value = operation.getExtensions().get(HANDLER_CLASS_EXTENSION);
        if (!(value instanceof String fqn) || fqn.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Class.forName(fqn));
        } catch (ClassNotFoundException e) {
            logger.warn("x-handler-class '{}' not found on classpath", fqn);
            return Optional.empty();
        }
    }

    private static HandlerType toHandlerType(io.swagger.v3.oas.models.PathItem.HttpMethod method) {
        return HandlerType.valueOf(method.name());
    }

    private static String toJavalinPath(String openApiPath) {
        return openApiPath.replaceAll("\\{([^/}]+)}", "{$1}");
    }

    private static HandlerContainer buildLocalContainer() {
        AmazonDynamoDB dynamoDb = LocalDynamoDb.startAndCreateTable(RESOURCES_TABLE_NAME);
        var environment = new Environment();
        var uriRetriever = new UriRetriever();
        var resourceService = new ResourceService(
            dynamoDb,
            RESOURCES_TABLE_NAME,
            Clock.systemDefaultZone(),
            uriRetriever,
            ChannelClaimClient.create(uriRetriever),
            new CustomerService(uriRetriever),
            (CristinUnitsUtil) unitId -> unitId);
        return new HandlerContainer()
                   .register(ResourceService.class, resourceService)
                   .register(Environment.class, environment)
                   .register(RawContentRetriever.class, new NoopRawContentRetriever())
                   .register(IdentityServiceClient.class, IdentityServiceClient.unauthorizedIdentityServiceClient())
                   .register(SecretsManagerClient.class, fakeSecretsManagerClient())
                   .register(HttpClient.class, HttpClient.newHttpClient());
    }

    private static SecretsManagerClient fakeSecretsManagerClient() {
        return (SecretsManagerClient) Proxy.newProxyInstance(
            SecretsManagerClient.class.getClassLoader(),
            new Class<?>[]{SecretsManagerClient.class},
            (proxy, method, args) -> {
                if ("getSecretValue".equals(method.getName())
                    && args != null && args.length == 1
                    && args[0] instanceof GetSecretValueRequest) {
                    return GetSecretValueResponse.builder().secretString(FAKE_CREDENTIALS_JSON).build();
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(
                    "Local adapter has no SecretsManager support for " + method.getName());
            });
    }

    private static final class NoopRawContentRetriever implements RawContentRetriever {

        @Override
        public Optional<String> getRawContent(URI uri, String mediaType) {
            return Optional.empty();
        }

        @Override
        public Optional<HttpResponse<String>> fetchResponse(URI uri, String mediaType) {
            return Optional.empty();
        }
    }
}
