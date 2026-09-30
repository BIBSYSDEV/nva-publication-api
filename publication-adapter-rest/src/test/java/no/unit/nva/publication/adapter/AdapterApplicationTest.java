package no.unit.nva.publication.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.UUID;
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
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String ACCEPT = "Accept";
    private static final String APPLICATION_JSON = "application/json";
    private static final String EMPTY_PUBLICATION = "{}";
    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String ID_FIELD = "id";
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.dtoObjectMapper;

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

    private static HttpResponse<String> get(String identifier) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("%s/%s".formatted(baseUri, identifier)))
                          .header(ACCEPT, APPLICATION_JSON)
                          .GET()
                          .build();
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
