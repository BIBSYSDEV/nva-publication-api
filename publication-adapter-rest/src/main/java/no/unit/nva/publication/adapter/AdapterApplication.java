package no.unit.nva.publication.adapter;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Comparator;
import java.util.Optional;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.DataCiteDoiClient;
import no.unit.nva.doi.DoiClient;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.external.services.ChannelClaimClient;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.file.upload.FileService;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.PublishingService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.services.UriResolver;
import no.unit.nva.publication.services.UriResolverImpl;
import no.unit.nva.publication.services.UriShortener;
import no.unit.nva.publication.services.UriShortenerImpl;
import no.unit.nva.publication.ticket.create.TicketResolver;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import nva.commons.core.Environment;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public final class AdapterApplication {

    private static final Logger logger = LoggerFactory.getLogger(AdapterApplication.class);
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_OPENAPI_PATH = "../docs/openapi.yaml";
    private static final String HANDLER_CLASS_EXTENSION = "x-handler-class";
    private static final String API_HOST_ENV = "API_HOST";
    private static final String BACKEND_CLIENT_SECRET_NAME_ENV = "BACKEND_CLIENT_SECRET_NAME";
    private static final String BACKEND_CLIENT_AUTH_URL_ENV = "BACKEND_CLIENT_AUTH_URL";
    private static final String SHORTENED_URI_TABLE_NAME_ENV = "SHORTENED_URI_TABLE_NAME";
    private static final String S3_FORCE_PATH_STYLE_ENV = "S3_FORCE_PATH_STYLE";
    private static final String STORAGE_BUCKET_ENV = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    private static final char PATH_PARAMETER_START = '{';
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.dtoObjectMapper;
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
        var openApi = readOpenApi(openApiPath);
        MockIntegrations.start(mockPort);
        var dynamoDb = DynamoDbClient.create();
        ResourceTable.createIfMissing(dynamoDb, RESOURCES_TABLE_NAME);
        ResourceTable.createShortenedUriTableIfMissing(
            dynamoDb, new Environment().readEnv(SHORTENED_URI_TABLE_NAME_ENV));
        createBucketIfMissing(new Environment().readEnv(STORAGE_BUCKET_ENV));
        var app = new AdapterApplication(buildContainer(dynamoDb), openApi);
        app.start(port);
    }

    public Javalin start(int port) {
        var javalin = Javalin.create();
        registerRoutes(javalin);
        javalin.start(port);
        logger.info("Adapter listening on port {}", javalin.port());
        return javalin;
    }

    private void registerRoutes(Javalin javalin) {
        // Javalin matches in registration order, so a literal path like /context has to be
        // registered before /{publicationIdentifier} or the parameterised route swallows it
        openApi.getPaths().entrySet().stream()
            .sorted(Comparator.comparingInt(entry -> pathParameterCount(entry.getKey())))
            .forEach(entry -> entry.getValue().readOperationsMap()
                                  .forEach((method, operation) ->
                                               registerOperation(javalin, entry.getKey(), method, operation)));
    }

    private void registerOperation(Javalin javalin, String path, HttpMethod method, Operation operation) {
        var handlerClass = resolveHandlerClass(operation);
        if (handlerClass.isEmpty()) {
            logger.info("Skipping {} {} (no x-handler-class)", method, path);
            return;
        }
        var javalinPath = toJavalinPath(path);
        javalin.addHttpHandler(toHandlerType(method), javalinPath, ctx -> invoke(handlerClass.get(), ctx));
        logger.info("Registered {} {} -> {}", method, javalinPath, handlerClass.get().getName());
    }

    private static int pathParameterCount(String path) {
        return (int) path.chars().filter(character -> character == PATH_PARAMETER_START).count();
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

    static OpenAPI readOpenApi(String openApiPath) {
        try {
            return Yaml.mapper().readValue(new File(openApiPath), OpenAPI.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse OpenAPI at " + openApiPath, e);
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

    private static HandlerType toHandlerType(HttpMethod method) {
        return HandlerType.valueOf(method.name());
    }

    private static String toJavalinPath(String openApiPath) {
        return openApiPath.replaceAll("\\{([^/}]+)}", "{$1}");
    }

    static HandlerContainer buildContainer(DynamoDbClient dynamoDb) {
        var environment = new Environment();
        var uriRetriever = new UriRetriever();
        var cristinUnitsUtil = (CristinUnitsUtil) unitId -> unitId;
        var resourceService = localResourceService(dynamoDb, uriRetriever, cristinUnitsUtil);
        var identityServiceClient = IdentityServiceClient.unauthorizedIdentityServiceClient();
        var ticketService = new TicketService(dynamoDb, uriRetriever, cristinUnitsUtil);
        var httpClient = HttpClient.newHttpClient();
        var secretsManagerClient = FileBackedSecretsManagerClient.fromEnvironment();
        var doiClient = new DataCiteDoiClient(httpClient, secretsManagerClient,
                                              environment.readEnv(API_HOST_ENV));
        var s3Client = S3Client.builder().forcePathStyle(forcePathStyle()).build();
        var customerApiClient = customerApiClient(environment, httpClient, secretsManagerClient);
        var uriShortener = UriShortenerImpl.createDefault(environment.readEnv(API_HOST_ENV));
        return new HandlerContainer()
                   .register(ResourceService.class, resourceService)
                   .register(TicketService.class, ticketService)
                   .register(MessageService.class,
                             new MessageService(dynamoDb, uriRetriever, cristinUnitsUtil))
                   .register(PublishingService.class,
                             new PublishingService(resourceService, ticketService, identityServiceClient))
                   .register(TicketResolver.class, new TicketResolver(resourceService, ticketService))
                   .register(Environment.class, environment)
                   .register(RawContentRetriever.class, new NoopRawContentRetriever())
                   .register(IdentityServiceClient.class, identityServiceClient)
                   .register(SecretsManagerClient.class, secretsManagerClient)
                   .register(HttpClient.class, httpClient)
                   .register(EventBridgeClient.class, new LoggingEventBridgeClient())
                   .register(DataCiteDoiClient.class, doiClient)
                   // Handlers declare the interface, and the container matches on exact type
                   .register(DoiClient.class, doiClient)
                   .register(S3Client.class, s3Client)
                   .register(S3Presigner.class,
                             S3Presigner.builder()
                                 .serviceConfiguration(S3Configuration.builder()
                                                           .pathStyleAccessEnabled(forcePathStyle())
                                                           .build())
                                 .build())
                   .register(CustomerApiClient.class, customerApiClient)
                   .register(FileService.class,
                             new FileService(s3Client, customerApiClient, resourceService))
                   .register(UriShortener.class, uriShortener)
                   .register(UriResolver.class,
                             new UriResolverImpl(dynamoDb,
                                                 environment.readEnv(SHORTENED_URI_TABLE_NAME_ENV)));
    }

    private static void createBucketIfMissing(String bucketName) {
        try (var s3Client = S3Client.builder().forcePathStyle(forcePathStyle()).build()) {
            s3Client.createBucket(request -> request.bucket(bucketName));
            logger.info("Created bucket {}", bucketName);
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
            logger.info("Bucket {} already exists", bucketName);
        }
    }

    /**
     * S3-compatible servers are reached by bucket-in-path rather than by subdomain, and the AWS SDK
     * has no environment variable for this, unlike the endpoint itself.
     */
    private static boolean forcePathStyle() {
        return Boolean.parseBoolean(System.getenv(S3_FORCE_PATH_STYLE_ENV));
    }

    private static ResourceService localResourceService(DynamoDbClient dynamoDb, UriRetriever uriRetriever,
                                                        CristinUnitsUtil cristinUnitsUtil) {
        return new ResourceService(
            dynamoDb,
            RESOURCES_TABLE_NAME,
            Clock.systemDefaultZone(),
            uriRetriever,
            ChannelClaimClient.create(uriRetriever),
            new CustomerService(uriRetriever),
            cristinUnitsUtil);
    }

    /**
     * JavaHttpClientCustomerApiClient.defaultInstance builds its own SecretsManager client, which
     * bypasses the one registered here and reaches for real AWS, so the client is assembled
     * explicitly instead.
     */
    private static CustomerApiClient customerApiClient(Environment environment, HttpClient httpClient,
                                                       SecretsManagerClient secretsManagerClient) {
        var credentials = new SecretsReader(secretsManagerClient)
                              .fetchClassSecret(environment.readEnv(BACKEND_CLIENT_SECRET_NAME_ENV),
                                                BackendClientCredentials.class);
        var cognitoCredentials = new CognitoCredentials(
            credentials::getId, credentials::getSecret,
            URI.create(environment.readEnv(BACKEND_CLIENT_AUTH_URL_ENV)));
        return new JavaHttpClientCustomerApiClient(
            AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient, cognitoCredentials));
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
