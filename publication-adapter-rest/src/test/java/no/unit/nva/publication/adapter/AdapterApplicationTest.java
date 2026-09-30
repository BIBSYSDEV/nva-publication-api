package no.unit.nva.publication.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.swagger.v3.oas.models.Operation;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AdapterApplicationTest {

    private static final int RANDOM_PORT = 0;
    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_NOT_FOUND = 404;
    private static final String OPENAPI_PATH = "../docs/openapi.yaml";
    private static final String HANDLER_CLASS_EXTENSION = "x-handler-class";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";
    private static final String EMPTY_PUBLICATION = "{}";
    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String ID_FIELD = "id";
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.dtoObjectMapper;

    // Operations deliberately left unwired. Anything else missing x-handler-class is a mistake.
    private static final Set<String> KNOWN_UNWIRED_OPERATIONS = Set.of(
        // Need S3 (MinIO or LocalStack) before they can run locally
        "POST /{publicationIdentifier}/file-upload/create",
        "POST /{publicationIdentifier}/file-upload/listparts",
        "POST /{publicationIdentifier}/file-upload/prepare",
        "POST /{publicationIdentifier}/file-upload/abort",
        "POST /{publicationIdentifier}/file-upload/complete",
        "POST /{publicationIdentifier}/file/{fileIdentifier}",
        "DELETE /{publicationIdentifier}/file/{fileIdentifier}",
        "GET /{publicationIdentifier}/filelink/{fileIdentifier}",
        "GET /file/{fileIdentifier}",
        // Handlers live in a module this one does not depend on
        "POST /{publicationIdentifier}/ticket/{ticketIdentifier}/message",
        "DELETE /{publicationIdentifier}/ticket/{ticketIdentifier}/message/{messageIdentifier}",
        // Construct fine, but would read the resources table instead of import candidates
        "GET /import-candidate/{importCandidateIdentifier}",
        "POST /import-candidate/{importCandidateIdentifier}",
        "PUT /import-candidate/{importCandidateIdentifier}",
        "GET /import-candidate/{importCandidateIdentifier}/file/{fileIdentifier}",
        // Needs a real DataCite registrar to do anything useful
        "POST /{publicationIdentifier}/doi");

    private static Javalin mocks;
    private static Javalin adapter;
    private static HttpClient httpClient;
    private static String baseUri;

    @BeforeAll
    static void startAdapter() {
        var mockPort = Integer.parseInt(System.getenv("MOCK_PORT"));
        mocks = MockIntegrations.start(mockPort);
        adapter = new AdapterApplication(AdapterApplication.buildLocalContainer(),
                                         AdapterApplication.readOpenApi(OPENAPI_PATH))
                      .start(RANDOM_PORT);
        baseUri = "http://localhost:%s".formatted(adapter.port());
        httpClient = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopAdapter() {
        adapter.stop();
        mocks.stop();
    }

    @Test
    void shouldInstantiateEveryHandlerDeclaredInOpenApi() {
        var container = AdapterApplication.buildLocalContainer();

        handlerClassesInOpenApi().forEach(handlerClass -> assertDoesNotThrow(
            () -> container.create(handlerClass),
            "%s is declared in openapi.yaml but cannot be constructed".formatted(handlerClass)));
    }

    @Test
    void shouldNotLeaveNewOperationsUnwiredWithoutSayingSo() {
        var unwired = operationsWithoutHandlerClass();

        assertThat("Operations in openapi.yaml have no x-handler-class and are not in "
                   + "KNOWN_UNWIRED_OPERATIONS. Either wire them up, or add them there with a reason.",
                   unwired, is(KNOWN_UNWIRED_OPERATIONS));
    }

    @Test
    void shouldNotLetIdentifierRouteSwallowLiteralPaths() throws IOException, InterruptedException {
        var response = get("context");

        assertThat(response.statusCode(), is(HTTP_OK));
    }

    @Test
    void shouldListPublicationsByOwner() throws IOException, InterruptedException {
        var response = getAuthenticated("by-owner");

        assertThat(response.statusCode(), is(HTTP_OK));
    }

    @Test
    void shouldListTicketsForPublication() throws IOException, InterruptedException {
        var identifier = identifierOf(createPublication());

        var response = getAuthenticated("%s/tickets".formatted(identifier));

        assertThat(response.statusCode(), is(HTTP_OK));
    }

    @Test
    void shouldFetchPublicationLog() throws IOException, InterruptedException {
        var identifier = identifierOf(createPublication());

        var response = getAuthenticated("%s/log".formatted(identifier));

        assertThat(response.statusCode(), is(HTTP_OK));
    }

    @Test
    void shouldReturnNotFoundForUnknownPublication() throws IOException, InterruptedException {
        var response = get(UUID.randomUUID().toString());

        assertThat(response.statusCode(), is(HTTP_NOT_FOUND));
    }

    @Test
    void shouldCreatePublicationWhenAuthorizerClaimsArePresent() throws IOException, InterruptedException {
        var response = createPublication();

        assertThat(response.statusCode(), is(HTTP_CREATED));
        assertThat(identifierOf(response), is(not(emptyIdentifier())));
    }

    @Test
    void shouldFetchPublicationCreatedThroughTheAdapter() throws IOException, InterruptedException {
        var identifier = identifierOf(createPublication());

        var response = get(identifier);

        assertThat(response.statusCode(), is(HTTP_OK));
        assertThat(response.body(), containsString(identifier));
    }

    private static HttpResponse<String> createPublication() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(baseUri + "/"))
                          .header(CONTENT_TYPE, APPLICATION_JSON)
                          .header(TestHeaderAuthorizerProvider.HEADER, authorizerClaims())
                          .POST(BodyPublishers.ofString(EMPTY_PUBLICATION))
                          .build();
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(getRequest(path).build());
    }

    private static HttpResponse<String> getAuthenticated(String path) throws IOException, InterruptedException {
        return send(getRequest(path)
                        .header(TestHeaderAuthorizerProvider.HEADER, authorizerClaims())
                        .build());
    }

    private static HttpRequest.Builder getRequest(String path) {
        return HttpRequest.newBuilder(URI.create("%s/%s".formatted(baseUri, path)))
                   .header(ACCEPT, APPLICATION_JSON)
                   .GET();
    }

    private static HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, BodyHandlers.ofString());
    }

    private static String identifierOf(HttpResponse<String> response) throws IOException {
        var body = OBJECT_MAPPER.readTree(response.body());
        if (body.has(IDENTIFIER_FIELD)) {
            return body.get(IDENTIFIER_FIELD).asText();
        }
        return lastPathElementOf(body.get(ID_FIELD).asText());
    }

    private static String lastPathElementOf(String uri) {
        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    private static String emptyIdentifier() {
        return "";
    }

    private static Set<String> operationsWithoutHandlerClass() {
        return AdapterApplication.readOpenApi(OPENAPI_PATH).getPaths().entrySet().stream()
                   .flatMap(path -> path.getValue().readOperationsMap().entrySet().stream()
                                        .filter(operation -> !declaresHandlerClass(operation.getValue()))
                                        .map(operation -> "%s %s".formatted(operation.getKey(), path.getKey())))
                   .collect(Collectors.toSet());
    }

    private static boolean declaresHandlerClass(Operation operation) {
        return nonNull(operation.getExtensions())
               && operation.getExtensions().get(HANDLER_CLASS_EXTENSION) instanceof String;
    }

    private static Stream<Class<?>> handlerClassesInOpenApi() {
        return AdapterApplication.readOpenApi(OPENAPI_PATH).getPaths().values().stream()
                   .flatMap(pathItem -> pathItem.readOperationsMap().values().stream())
                   .map(Operation::getExtensions)
                   .filter(Objects::nonNull)
                   .map(extensions -> extensions.get(HANDLER_CLASS_EXTENSION))
                   .filter(String.class::isInstance)
                   .map(String.class::cast)
                   .map(AdapterApplicationTest::classForName);
    }

    private static Class<?> classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("x-handler-class not on classpath: " + className, e);
        }
    }

    private static String authorizerClaims() {
        var customerId = "%s/customer/550e8400-e29b-41d4-a716-446655440000"
                             .formatted(System.getenv("COGNITO_HOST"));
        return OBJECT_MAPPER.createObjectNode()
                   .put("custom:customerId", customerId)
                   .put("custom:nvaUsername", "testuser@sikt.no")
                   .put("custom:accessRights", "MANAGE_OWN_RESOURCES")
                   .put("custom:cristinId", "https://api.cristin.no/person/12345")
                   .put("custom:topOrgCristinId", "https://api.cristin.no/organization/7482.0.0.0")
                   .put("custom:personAffiliation", "https://api.cristin.no/organization/7482.0.0.0")
                   .toString();
    }
}
