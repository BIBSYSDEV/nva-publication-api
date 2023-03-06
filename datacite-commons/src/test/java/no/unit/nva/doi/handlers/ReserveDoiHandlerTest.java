package no.unit.nva.doi.handlers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.net.HttpHeaders.ACCEPT;
import static no.unit.nva.doi.DataCiteReserveDoiClient.DOI_REGISTRAR;
import static no.unit.nva.doi.ReserveDoiRequestValidator.DOI_ALREADY_EXISTS_ERROR_MESSAGE;
import static no.unit.nva.doi.ReserveDoiRequestValidator.NOT_DRAFT_STATUS_ERROR_MESSAGE;
import static no.unit.nva.doi.ReserveDoiRequestValidator.UNSUPPORTED_ROLE_ERROR_MESSAGE;
import static no.unit.nva.doi.handlers.ReserveDoiHandler.BAD_RESPONSE_ERROR_MESSAGE;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.DataCiteReserveDoiClient;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
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
    public static final String EXPECTED_BAD_REQUEST_RESPONSE_MESSAGE = "ExpectedResponseMessage";
    private final Environment environment = mock(Environment.class);
    private Context context;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private ReserveDoiHandler handler;

    @BeforeEach
    public void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) {
        super.init();
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv("API_HOST")).thenReturn(wireMockRuntimeInfo.getHttpsBaseUrl());
        context = mock(Context.class);
        output = new ByteArrayOutputStream();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        DataCiteReserveDoiClient reserveDoiClient = new DataCiteReserveDoiClient(WiremockHttpClient.create(),
                                                                                 environment);
        handler = new ReserveDoiHandler(resourceService, reserveDoiClient, environment);
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
    void shouldReturnBadMethodExceptionWhenPublicationAlreadyHasDoi() throws IOException, ApiGatewayException {
        var publication = createPersistedDraftPublicationWithDoi();
        var request = generateRequestWithOwner(publication, OWNER);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertEquals(HttpURLConnection.HTTP_BAD_METHOD, response.getStatusCode());
        assertThat(response.getBodyObject(Problem.class).getDetail(),
                   is(equalTo(DOI_ALREADY_EXISTS_ERROR_MESSAGE)));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenUserIsNotAnOwner() throws ApiGatewayException, IOException {
        var publication = createPersistedDraftPublication();
        var request = generateRequestWithOwner(publication, NOT_OWNER);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.getStatusCode());
        assertThat(response.getBodyObject(Problem.class).getDetail(), is(equalTo(UNSUPPORTED_ROLE_ERROR_MESSAGE)));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenResourceDoesNotExist() throws IOException {
        var publication = createNotPersistedDraftPublication();
        var request = generateRequestWithOwner(publication, OWNER);
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.getStatusCode());
        assertThat(response.getBodyObject(Problem.class).getDetail(),
                   is(equalTo(NOT_FOUND_MESSAGE + publication.getIdentifier())));
    }

    @Test
    void shouldReturnBadResponseWhenBadResponseFromDoiRegistrar()
        throws ApiGatewayException, IOException {
        var publication = createPersistedDraftPublication();
        var request = generateRequestWithOwner(publication, OWNER);
        mockReserveDoiFailedResponse();
        handler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertEquals(HttpURLConnection.HTTP_BAD_GATEWAY, response.getStatusCode());
        assertThat(response.getBodyObject(Problem.class).getDetail(), is(equalTo(BAD_RESPONSE_ERROR_MESSAGE)));
    }

    @Test
    void shouldReturnDoiSuccessfully() throws IOException, ApiGatewayException {
        var publication = createPersistedDraftPublication();
        var expectedDoi = URI.create("https://doiHost/10.0000/" + randomString());
        var request = generateRequestWithOwner(publication, OWNER);
        mockReserveDoiResponse(createResponse(expectedDoi.toString()));
        handler.handleRequest(request, output, context);
        var updatedPublication = resourceService.getPublication(publication);

        assertThat(updatedPublication.getDoi(), is(equalTo(expectedDoi)));
        var response = GatewayResponse.fromOutputStream(output, DoiResponse.class);
        assertEquals(HttpURLConnection.HTTP_CREATED, response.getStatusCode());
        var actualDoi = response.getBodyObject(DoiResponse.class);
        assertThat(actualDoi.getDoi(), is(equalTo(expectedDoi)));
    }

    private static String createResponse(String expectedDoiPrefix) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.writeValueAsString(new DoiResponse(URI.create(expectedDoiPrefix)));
    }

    private Publication createPersistedDraftPublicationWithDoi() throws NotFoundException {
        Publication publication = PublicationGenerator.randomPublication();
        publication.setResourceOwner(new ResourceOwner(ReserveDoiHandlerTest.OWNER, randomUri()));
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
            Resource.fromPublication(publication).persistNew(resourceService, userInstance).getIdentifier();
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createNotPersistedDraftPublication() {
        Publication publication = PublicationGenerator.randomPublication();
        publication.setDoi(null);
        publication.setResourceOwner(new ResourceOwner(NOT_OWNER, randomUri()));
        return publication;
    }

    private Publication createPersistedDraftPublication() throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        publication.setDoi(null);
        publication.setResourceOwner(new ResourceOwner(ReserveDoiHandlerTest.OWNER, randomUri()));
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
            Resource.fromPublication(publication).persistNew(resourceService, userInstance).getIdentifier();
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Publication createPersistedPublishedPublication() throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        publication.setDoi(null);
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
        stubFor(post(urlPathEqualTo("/" + DOI_REGISTRAR)).willReturn(WireMock.ok().withStatus(HttpURLConnection.HTTP_OK)
                                    .withBody(getDoiResponseJson)));
    }

    private void mockReserveDoiFailedResponse() {
        stubFor(post(urlPathEqualTo(RandomDataGenerator.randomUri().getPath()))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_FORBIDDEN)
                                    .withBody(EXPECTED_BAD_REQUEST_RESPONSE_MESSAGE)));
    }
}
