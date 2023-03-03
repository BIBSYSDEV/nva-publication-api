package no.unit.nva.doi.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.unit.nva.doi.handlers.ReserveDoiHandler.DATACITE_CONFIG_ERROR;
import static no.unit.nva.doi.handlers.ReserveDoiHandler.NOT_DRAFT_STATUS_ERROR_MESSAGE;
import static no.unit.nva.doi.handlers.ReserveDoiHandler.UNSUPPORTED_ROLE_ERROR_MESSAGE;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

@WireMockTest(httpsEnabled = true)
public class ReserveDoiHandlerTest extends ResourcesLocalTest {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String DOI = "doi";
    public static final String NOT_OWNER = "notOwner";
    public static final String OWNER = "owner";
    public static final String NOT_FOUND_MESSAGE = "Publication not found: ";
    public static final String DATACITE_SECRET_NAME = "dataCiteCustomerSecrets";
    public static final String DATACITE_SECRET_KEY = "dataCiteCustomerSecrets";
    public static final String DATACITE_CONFIG = IoUtils.stringFromResources(Path.of("dataCiteConfig.json"));
    public static final String EXPECTED_BAD_REQUEST_RESPONSE_MESSAGE = "ExpectedResponseMessage";
    private static final String DOIS_PATH_PREFIX = "/dois";
    private static final String MDS_PASSWORD = "somePassword";
    private static final String MDS_USERNAME = "someUsername";
    private static final String APPLICATION_VND_API_JSON = "application/vnd.api+json";
    private Context context;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private FakeSecretsManagerClient fakeSecretsManagerClient;
    private ReserveDoiHandler handler;

    @BeforeEach
    public void setUp() {
        super.init();
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        context = mock(Context.class);
        output = new ByteArrayOutputStream();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        fakeSecretsManagerClient = new FakeSecretsManagerClient();
        fakeSecretsManagerClient.putSecret(DATACITE_SECRET_NAME, DATACITE_SECRET_KEY, DATACITE_CONFIG);
        handler = new ReserveDoiHandler(resourceService, fakeSecretsManagerClient, environment);
    }

    @Test
    void shouldThrowBadMethodExceptionWhenPublicationIsNotADraft() throws ApiGatewayException, IOException {
        var publication = createPersistedPublishedPublication();
        var request = generateRequest(publication);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertEquals(HttpURLConnection.HTTP_BAD_METHOD, response.getStatusCode());
        assertThat(response.getBodyObject(Problem.class).getDetail(), is(equalTo(NOT_DRAFT_STATUS_ERROR_MESSAGE)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenUserIsNotAnOwner() throws ApiGatewayException, IOException {
        var publication = createPersistedDraftPublicationWithOwner(OWNER);
        var request = generateRequestWithOwner(publication, NOT_OWNER);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());
        assertThat(response.getBodyObject(Problem.class).getDetail(), is(equalTo(UNSUPPORTED_ROLE_ERROR_MESSAGE)));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenResourceDoesNotExist() throws IOException, NotFoundException {
        var publication = createNotPersistedDraftPublication();
        var request = generateRequestWithOwner(publication, OWNER);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode());
        assertThat(response.getBodyObject(Problem.class).getDetail(),
                   is(equalTo(NOT_FOUND_MESSAGE + publication.getIdentifier())));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenUsersInstitutionHasNoDataciteConfig()
        throws ApiGatewayException, IOException {
        var publication = createPersistedDraftPublicationWithOwner(OWNER);
        var request = generateRequestWithOwner(publication, OWNER);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());
        assertThat(response.getBodyObject(Problem.class).getDetail(), is(equalTo(DATACITE_CONFIG_ERROR)));
    }

    private Publication createNotPersistedDraftPublication() {
        Publication publication = PublicationGenerator.randomPublication();
        publication.setResourceOwner(new ResourceOwner(NOT_OWNER, randomUri()));
        return publication;
    }

    private Publication createPersistedDraftPublicationWithOwner(String owner) throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        publication.setResourceOwner(new ResourceOwner(owner, randomUri()));
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
            Resource.fromPublication(publication).persistNew(resourceService, userInstance).getIdentifier();
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createPersistedPublishedPublication() throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
            Resource.fromPublication(publication).persistNew(resourceService, userInstance).getIdentifier();
        resourceService.publishPublication(userInstance, publicationIdentifier);
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private InputStream generateRequest(Publication publication) throws JsonProcessingException {
        Map<String, String> headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString(),
                                                    DOI, DOI);
        return new HandlerRequestBuilder<InputStream>(JsonUtils.dtoObjectMapper)
                   .withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .build();
    }

    private InputStream generateRequestWithOwner(Publication publication, String owner) throws JsonProcessingException {
        Map<String, String> headers = Map.of(ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString(),
                                                    DOI, DOI);
        return new HandlerRequestBuilder<InputStream>(JsonUtils.dtoObjectMapper)
                   .withHeaders(headers)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withNvaUsername(owner)
                   .build();
    }

    private void mockReserveDoiResponse(String getDoiResponseJson) {
        stubFor(get(urlEqualTo(""))
                    .withBasicAuth(MDS_USERNAME, MDS_PASSWORD)
                    .willReturn(aResponse()
                                    .withHeader(CONTENT_TYPE, APPLICATION_VND_API_JSON)
                                    .withStatus(HttpStatus.SC_OK)
                                    .withBody(getDoiResponseJson)));
    }

    private void mockReserveDoiFailedResponse() {
        stubFor(post(urlEqualTo(DOIS_PATH_PREFIX))
                    .withBasicAuth(MDS_USERNAME, MDS_PASSWORD)
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_FORBIDDEN)
                                    .withBody(EXPECTED_BAD_REQUEST_RESPONSE_MESSAGE)));
    }
}
