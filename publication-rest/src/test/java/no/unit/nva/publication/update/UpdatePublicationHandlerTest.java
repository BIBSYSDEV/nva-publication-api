package no.unit.nva.publication.update;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.RequestUtil.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.useraccessserivce.accessrights.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

public class UpdatePublicationHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String IDENTIFIER = "identifier";

    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE =
        restApiMapper.getTypeFactory().constructParametricType(GatewayResponse.class, Problem.class);

    public static final String SOME_MESSAGE = "SomeMessage";
    public static final String SOME_CURATOR = "some@curator";

    private ResourceService publicationService;
    private Context context;

    private ByteArrayOutputStream output;
    private UpdatePublicationHandler updatePublicationHandler;

    private Publication publication;
    private Environment environment;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        super.init();

        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");

        publicationService = new ResourceService(client, Clock.systemDefaultZone());
        context = mock(Context.class);

        output = new ByteArrayOutputStream();
        updatePublicationHandler =
            new UpdatePublicationHandler(publicationService, environment);
        publication = createPublication();
    }

    @Test
    public void handlerUpdatesPublicationWhenInputIsValidAndUserIsResourceOwner() throws IOException,
                                                                                         ApiGatewayException {
        publication = PublicationGenerator.publicationWithoutIdentifier();
        Publication savedPublication = publicationService.createPublication(publication);

        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);
        GatewayResponse<PublicationResponse> gatewayResponse = GatewayResponse.fromOutputStream(output);
        final PublicationResponse body = gatewayResponse.getBodyObject(PublicationResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        assertThat(body.getEntityDescription().getMainTitle(),
                   is(equalTo(publicationUpdate.getEntityDescription().getMainTitle())));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    public void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        InputStream event = generateInputStreamMissingPathParameters().build();
        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(IDENTIFIER_IS_NOT_A_VALID_UUID));
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    public void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        publicationService = serviceFailsOnModifyRequestWithRuntimeError();
        updatePublicationHandler = new UpdatePublicationHandler(publicationService, environment);

        Publication savedPublication = publicationService.createPublication(publication);
        InputStream event = ownerUpdatesOwnPublication(savedPublication.getIdentifier(), savedPublication);
        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("handler logs error details on unexpected exception")
    public void handlerLogsErrorDetailsOnUnexpectedException()
        throws IOException, ApiGatewayException {
        final TestAppender appender = createAppenderForLogMonitoring();
        publicationService = serviceFailsOnModifyRequestWithRuntimeError();
        updatePublicationHandler = new UpdatePublicationHandler(publicationService, environment);
        Publication savedPublication = publicationService.createPublication(publication);

        InputStream event = ownerUpdatesOwnPublication(savedPublication.getIdentifier(), savedPublication);
        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(appender.getMessages(), containsString(SOME_MESSAGE));
    }

    @Test
    public void handlerReturnsBadRequestWhenIdentifierInPathDiffersFromIdentifierInBody() throws IOException {

        SortableIdentifier someOtherIdentifier = SortableIdentifier.next();

        InputStream event = ownerUpdatesOwnPublication(someOtherIdentifier, publication);

        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(output);
        Problem problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(UpdatePublicationHandler.IDENTIFIER_MISMATCH_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("Handler returns NotFound response when resource does not exist")
    void handlerReturnsNotFoundResponseWhenResourceDoesNotExist() throws IOException {
        InputStream event = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);

        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_NOT_FOUND, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), is(equalTo(RESOURCE_NOT_FOUND_MESSAGE)));
    }

    @Test
    void handlerUpdatesResourceWhenInputIsValidAndUserHasRightToEditAnyResourceInOwnInstitution()
        throws TransactionFailedException, IOException, NotFoundException {
        Publication savedPublication = publicationService.createPublication(publication);
        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream event = userUpdatesPublicationAndHasRightToUpdate(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        Publication updatedPublication =
            publicationService.getPublicationByIdentifier(savedPublication.getIdentifier());

        //inject modified date to the input object because modified date is not available before the actual update.
        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        String expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        String actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));

        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @Test
    void handlerThrowsExceptionWhenInputIsValidUserHasRightToEditAnyResourceInOwnInstButEditsResourceInOtherInst()
        throws TransactionFailedException, IOException {
        Publication savedPublication = publicationService.createPublication(publication);
        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream event = userUpdatesPublicationOfOtherInstitution(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> response = GatewayResponse.fromOutputStream(output);
        Problem problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
        assertThat(problem.getDetail(), is(equalTo("Unauthorized")));
    }

    private InputStream userUpdatesPublicationOfOtherInstitution(Publication publicationUpdate)
        throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, publicationUpdate.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
            .withFeideId(SOME_CURATOR)
            .withPathParameters(pathParameters)
            .withCustomerId(randomUri().toString())
            .withBody(publicationUpdate)
            .withAccessRight(AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.toString())
            .build();
    }

    private InputStream userUpdatesPublicationAndHasRightToUpdate(Publication publicationUpdate)
        throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, publicationUpdate.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
            .withFeideId(SOME_CURATOR)
            .withPathParameters(pathParameters)
            .withCustomerId(publicationUpdate.getPublisher().getId().toString())
            .withBody(publicationUpdate)
            .withAccessRight(AccessRight.EDIT_OWN_INSTITUTION_RESOURCES.toString())
            .build();
    }

    private InputStream ownerUpdatesOwnPublication(SortableIdentifier publicationIdentifier,
                                                   Publication publicationUpdate)
        throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(IDENTIFIER, publicationIdentifier.toString());

        return new HandlerRequestBuilder<Publication>(restApiMapper)
            .withFeideId(publicationUpdate.getOwner())
            .withCustomerId(publicationUpdate.getPublisher().getId().toString())
            .withBody(publicationUpdate)
            .withPathParameters(pathParameters)
            .build();
    }

    private Publication updateTitle(Publication savedPublication) {
        Publication update = savedPublication.copy().build();
        update.getEntityDescription().setMainTitle(randomString());
        return update;
    }

    private TestAppender createAppenderForLogMonitoring() {
        return LogUtils.getTestingAppenderForRootLogger();
    }

    private ResourceService serviceFailsOnModifyRequestWithRuntimeError() {
        return new ResourceService(client, Clock.systemDefaultZone()) {
            @Override
            public Publication updatePublication(Publication publicationUpdate) {
                throw new RuntimeException(SOME_MESSAGE);
            }
        };
    }

    private HandlerRequestBuilder<Publication> generateInputStreamMissingPathParameters() throws IOException {
        return new HandlerRequestBuilder<Publication>(restApiMapper)
            .withBody(createPublication())
            .withHeaders(generateHeaders());
    }

    private Map<String, String> generateHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return headers;
    }

    private Publication createPublication() {
        return PublicationGenerator.publicationWithIdentifier();
    }

    private GatewayResponse<Problem> toGatewayResponseProblem() throws JsonProcessingException {
        return restApiMapper.readValue(output.toString(),
                                       PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE);
    }

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }
}
