package no.unit.nva.publication.update;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomEntityDescription;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.listPublicationInstanceTypes;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseAcceptingFilesForAllTypes;
import static no.unit.nva.publication.CustomerApiStubs.stubCustomerResponseNotFound;
import static no.unit.nva.publication.CustomerApiStubs.stubSuccessfulCustomerResponseAllowingFilesForNoTypes;
import static no.unit.nva.publication.CustomerApiStubs.stubSuccessfulTokenResponse;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.RequestUtil.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.delete.DeletePublicationHandler.LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS;
import static no.unit.nva.publication.delete.DeletePublicationHandler.NVA_PUBLICATION_DELETE_SOURCE;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.USER;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.License;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.model.instancetypes.degree.DegreeMaster;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.delete.LambdaDestinationInvocationDetail;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.FileForApproval;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.http.RandomPersonServiceResponse;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.http.entity.ContentType;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@WireMockTest(httpsEnabled = true)
class UpdatePublicationHandlerTest extends ResourcesLocalTest {

    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE =
        restApiMapper.getTypeFactory().constructParametricType(GatewayResponse.class, Problem.class);

    public static final String SOME_MESSAGE = "SomeMessage";
    public static final String SOME_CURATOR = "some@curator";
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    public static final String NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    public static final String EVENT_BUS_NAME = "EVENT_BUS_NAME";
    private static final String API_HOST = "API_HOST";
    private static final String PUBLICATION = "publication";
    private final GetExternalClientResponse getExternalClientResponse = mock(GetExternalClientResponse.class);
    private final Context context = new FakeContext();
    private ResourceService publicationService;
    private ByteArrayOutputStream output;
    private UpdatePublicationHandler updatePublicationHandler;
    private Publication publication;
    private Environment environment;
    private IdentityServiceClient identityServiceClient;
    private TicketService ticketService;
    private FakeSecretsManagerClient secretsManagerClient;
    private FakeEventBridgeClient eventBridgeClient;
    private S3Client s3Client;
    private URI customerId;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) throws NotFoundException {
        super.init();

        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv(NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY)).thenReturn(
            NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY);
        lenient().when(environment.readEnv("BACKEND_CLIENT_SECRET_NAME")).thenReturn("secret");

        var baseUrl = URI.create(wireMockRuntimeInfo.getHttpsBaseUrl());
        lenient().when(environment.readEnv("BACKEND_CLIENT_AUTH_URL"))
            .thenReturn(baseUrl.getHost() + ":" + baseUrl.getPort());

        publicationService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);

        this.eventBridgeClient = new FakeEventBridgeClient(EVENT_BUS_NAME);

        identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);

        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("secret", credentials.toString());
        output = new ByteArrayOutputStream();
        s3Client = mock(S3Client.class);
        var httpClient = WiremockHttpClient.create();
        updatePublicationHandler =
            new UpdatePublicationHandler(publicationService, ticketService, environment, identityServiceClient,
                                         eventBridgeClient, s3Client, secretsManagerClient, httpClient);
        publication = createNonDegreePublication();

        customerId = UriWrapper.fromUri(wireMockRuntimeInfo.getHttpsBaseUrl())
                         .addChild("customer", UUID.randomUUID().toString())
                         .getUri();

        publication = randomPublicationWithPublisher(customerId);

        stubSuccessfulTokenResponse();
        stubCustomerResponseAcceptingFilesForAllTypes(customerId);
    }

    private static Publication randomPublicationWithPublisher(URI customerId) {
        return randomPublication()
                   .copy()
                   .withPublisher(new Organization.Builder()
                                      .withId(customerId)
                                      .build())
                   .build();
    }

    private static Publication randomPublicationWithPublisher(URI customerId, Class<?> publicationInstanceClass) {
        return randomPublication(publicationInstanceClass)
                   .copy()
                   .withPublisher(new Organization.Builder()
                                      .withId(customerId)
                                      .build())
                   .build();
    }

    static Stream<Arguments> allDegreeInstances() {
        return Stream.of(
            Arguments.of(DegreeMaster.class),
            Arguments.of(DegreeBachelor.class),
            Arguments.of(DegreePhd.class)
        );
    }

    @Test
    void handlerUpdatesPublicationWhenInputIsValidAndUserIsResourceOwner()
        throws IOException, ApiGatewayException {
        publication = publicationWithoutIdentifier(customerId);
        Publication savedPublication = createSamplePublication();

        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HTTP_OK)));

        final PublicationResponse body = gatewayResponse.getBodyObject(PublicationResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(body.getEntityDescription().getMainTitle(),
                   is(equalTo(publicationUpdate.getEntityDescription().getMainTitle())));
    }

    @Test
    void handlerCreatesPendingPublishingRequestTicketForPublishedPublicationWhenUpdatingFiles()
        throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedNonDegreePublication(customerId,
                                                                              PublicationStatus.PUBLISHED,
                                                                              publicationService);

        var publicationUpdate = addAnotherUnpublishedFile(publishedPublication);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        var ticket = ticketService.fetchTicketByResourceIdentifier(publicationUpdate.getPublisher().getId(),
                                                                   publicationUpdate.getIdentifier(),
                                                                   PublishingRequestCase.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(ticket.map(PublishingRequestCase::getStatus).orElseThrow(), is(equalTo(PENDING)));
    }

    @Test
    void handlerCreatesPendingPublishingRequestTicketForPublishedPublicationWhenCompletedPublishingRequestExists()
        throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(customerId,
                                                                              PublicationStatus.PUBLISHED,
                                                                              publicationService);
        var completedTicket = persistCompletedPublishingRequest(publishedPublication);
        var publicationUpdate = addAnotherUnpublishedFile(publishedPublication);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        var tickets = ticketService.fetchTicketsForUser(UserInstance.fromTicket(completedTicket))
                          .collect(Collectors.toList());
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertTrue(containsOneCompletedAndOnePendingPublishingRequest(tickets));
    }

    @Test
    void handlerDoesNotCreateNewPublishingRequestWhenThereExistsPendingPublishingRequest()
        throws IOException, ApiGatewayException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(customerId,
                                                                              PublicationStatus.PUBLISHED,
                                                                              publicationService);
        var pendingTicket = createPendingPublishingRequest(publishedPublication);
        var publicationUpdate = addAnotherUnpublishedFile(publishedPublication);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);

        var existingTickets = ticketService.fetchTicketsForUser(UserInstance.fromTicket(pendingTicket))
                                  .collect(Collectors.toList());
        assertThat(existingTickets, hasSize(1));
    }

    @Test
    void handlerDoesNotCreateNewPublishingRequestWhenThereExistsPendingAndCompletedPublishingRequest()
        throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(customerId,
                                                                              PublicationStatus.PUBLISHED,
                                                                              publicationService);
        persistCompletedPublishingRequest(publishedPublication);
        var pendingPublishingRequest = createPendingPublishingRequest(publishedPublication);

        var publicationUpdate = addAnotherUnpublishedFile(publishedPublication);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        updatePublicationHandler.handleRequest(inputStream, output, context);

        var existingTickets = ticketService.fetchTicketsForUser(UserInstance.fromTicket(pendingPublishingRequest))
                                  .collect(Collectors.toList());
        assertThat(existingTickets, hasSize(2));
    }

    @Test
    void handlerUpdatesPublicationWhenInputIsValidAndUserIsExternalClient() throws IOException, BadRequestException {
        publication.setIdentifier(null);
        Publication savedPublication = createSamplePublication();

        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream inputStream =
            externalClientUpdatesPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        when(getExternalClientResponse.getCustomerUri())
            .thenReturn(publication.getPublisher().getId());
        when(getExternalClientResponse.getActingUser())
            .thenReturn(publication.getResourceOwner().getOwner().getValue());

        updatePublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        final PublicationResponse body = gatewayResponse.getBodyObject(PublicationResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        assertThat(body.getEntityDescription().getMainTitle(),
                   is(equalTo(publicationUpdate.getEntityDescription().getMainTitle())));
    }

    @Test
    @DisplayName("handler Returns BadRequest Response On Missing Path Param")
    void handlerReturnsBadRequestResponseOnMissingPathParam() throws IOException {
        InputStream event = generateInputStreamMissingPathParameters().build();
        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(IDENTIFIER_IS_NOT_A_VALID_UUID));
    }

    @Test
    @DisplayName("handler Returns InternalServerError Response On Unexpected Exception")
    void handlerReturnsInternalServerErrorResponseOnUnexpectedException()
        throws IOException, ApiGatewayException {
        publicationService = serviceFailsOnModifyRequestWithRuntimeError();

        updatePublicationHandler = new UpdatePublicationHandler(publicationService,
                                                                ticketService,
                                                                environment,
                                                                identityServiceClient,
                                                                eventBridgeClient,
                                                                S3Driver.defaultS3Client().build(),
                                                                secretsManagerClient,
                                                                WiremockHttpClient.create());

        Publication savedPublication = createSamplePublication();
        InputStream event = ownerUpdatesOwnPublication(savedPublication.getIdentifier(), savedPublication);
        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("handler logs error details on unexpected exception")
    void handlerLogsErrorDetailsOnUnexpectedException()
        throws IOException, ApiGatewayException {
        final TestAppender appender = createAppenderForLogMonitoring();
        publicationService = serviceFailsOnModifyRequestWithRuntimeError();
        updatePublicationHandler = new UpdatePublicationHandler(publicationService,
                                                                ticketService,
                                                                environment,
                                                                identityServiceClient,
                                                                eventBridgeClient,
                                                                S3Driver.defaultS3Client().build(),
                                                                secretsManagerClient,
                                                                WiremockHttpClient.create());

        var savedPublication = createSamplePublication();

        var event = ownerUpdatesOwnPublication(savedPublication.getIdentifier(), savedPublication, randomUri());
        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = toGatewayResponseProblem();
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(SC_INTERNAL_SERVER_ERROR)));
        assertThat(appender.getMessages(), containsString(SOME_MESSAGE));
    }

    @Test
    void handlerReturnsBadRequestWhenIdentifierInPathDiffersFromIdentifierInBody()
        throws IOException {
        var randpomPersistedPublication = buildPublication(p -> p
                                                                    .map(a -> setOwnerFromPublication(a, publication))
                                                                    .map(this::persistPublication));

        InputStream event = ownerUpdatesOwnPublication(randpomPersistedPublication.getIdentifier(), publication);

        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        Problem problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(problem.getDetail(), containsString(UpdatePublicationHandler.IDENTIFIER_MISMATCH_ERROR_MESSAGE));
    }

    private Builder persistPublication(Builder b) {
        try {
            return Resource
                       .fromPublication(b.build())
                       .persistNew(publicationService, UserInstance.fromPublication(publication))
                       .copy();
        } catch (BadRequestException e) {
            throw new RuntimeException(e);
        }
    }

    private Publication buildPublication(Function<Stream<Publication.Builder>, Stream<Publication.Builder>> p) {
        return p.apply(Stream.of(randomPublication().copy())).map(Builder::build).findFirst().orElseThrow();
    }

    private static Builder setOwnerFromPublication(Publication.Builder builder, Publication publication) {
        return builder
                   .withResourceOwner(publication.getResourceOwner())
                   .withPublisher(publication.getPublisher());
    }

    @Test
    @DisplayName("Handler returns NotFound response when resource does not exist")
    void handlerReturnsNotFoundResponseWhenResourceDoesNotExist() throws IOException {
        InputStream event = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);

        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(HTTP_NOT_FOUND, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), is(equalTo(RESOURCE_NOT_FOUND_MESSAGE)));
    }

    @Test
    void handlerUpdatesResourceWhenInputIsValidAndUserHasRightToEditAnyResourceInOwnInstitution()
        throws ApiGatewayException, IOException {
        Publication savedPublication = createSamplePublication();
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
        throws IOException, BadRequestException {
        Publication savedPublication = createSamplePublication();
        Publication publicationUpdate = updateTitle(savedPublication);
        InputStream event = userUpdatesPublicationOfOtherInstitution(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        Problem problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
        assertThat(problem.getDetail(), is(equalTo("Unauthorized")));
    }

    @Test
    void handlerReturnsForbiddenWhenExternalClientTriesToUpdateResourcesCreatedByOthers()
        throws IOException, BadRequestException {
        Publication savedPublication = createSamplePublication();
        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream inputStream =
            externalClientUpdatesPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        when(getExternalClientResponse.getCustomerUri()).thenReturn(randomUri());
        when(getExternalClientResponse.getActingUser()).thenReturn(randomString());

        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserCannotBeIdentified() throws IOException, BadRequestException {
        Publication savedPublication = createSamplePublication();
        var event = requestWithoutUsername(savedPublication);
        updatePublicationHandler.handleRequest(event, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldUpdateResourceWhenAuthorizedUserIsContributorAndHasCristinId()
        throws BadRequestException, IOException, NotFoundException {
        Publication savedPublication = createSamplePublication();
        injectRandomContributorsWithoutCristinIdAndIdentity(savedPublication);
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        injectContributor(savedPublication, contributor);
        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream event = contributorUpdatesPublicationAndHasRightsToUpdate(publicationUpdate, cristinId);
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
    void shouldReturnNotFoundWhenContributorUpdatesResourceThatDoesNotExist()
        throws BadRequestException, IOException {
        Publication savedPublication = createSamplePublication();
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        injectContributor(savedPublication, contributor);
        Publication nonExistentPublication = savedPublication.copy().withIdentifier(SortableIdentifier.next()).build();

        InputStream event = contributorUpdatesPublicationAndHasRightsToUpdate(nonExistentPublication, cristinId);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_NOT_FOUND)));
    }

    @Test
    void shouldReturnForbiddenWhenContributorWithoutCristinIdUpdatesResource()
        throws BadRequestException, IOException {
        Publication savedPublication = createSamplePublication();
        var contributor = createContributorForPublicationUpdate(null);
        injectContributor(savedPublication, contributor);
        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream event = contributorUpdatesPublicationWithoutHavingRights(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnForbiddenWhenContributorUpdatesResourceWithoutEntityDescription()
        throws BadRequestException, IOException {
        var savedPublication = Resource
                                   .fromPublication(new Publication())
                                   .persistNew(publicationService, UserInstance.fromPublication(publication));
        var cristinId = randomUri();

        InputStream event = contributorUpdatesPublicationAndHasRightsToUpdate(savedPublication, cristinId);
        updatePublicationHandler.handleRequest(event, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    @DisplayName("Handler returns OK when thesis and is owner")
    void shouldReturnOKWhenUserIsOwner() throws IOException, BadRequestException {
        var thesisPublication = publication.copy().withEntityDescription(thesisPublishableEntityDescription()).build();
        var savedThesis = Resource
                              .fromPublication(thesisPublication)
                              .persistNew(publicationService, UserInstance.fromPublication(publication));
        var updatedPublication = updateTitle(savedThesis);
        var input = ownerUpdatesOwnPublication(updatedPublication.getIdentifier(), updatedPublication);
        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HTTP_OK)));
    }

    @Test
    @DisplayName("Handler returns OK when thesis and user has PUBLISH_THESIS")
    void shouldReturnOKWhenUserHasPublishThesis() throws IOException, BadRequestException {
        var thesisPublication = publication.copy().withEntityDescription(thesisPublishableEntityDescription()).build();
        var savedThesis = Resource
                              .fromPublication(thesisPublication)
                              .persistNew(publicationService, UserInstance.fromPublication(publication));
        var updatedPublication = updateTitle(savedThesis);
        var input = userUpdatesPublicationAndHasRightToUpdate(updatedPublication);
        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HTTP_OK)));
    }

    @Test
    void shouldReturnBadGatewayWhenHttpClientUnableToRetrievePublishingWorkflow()
        throws IOException, ApiGatewayException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(customerId,
                                                                              PublicationStatus.PUBLISHED,
                                                                              publicationService);

        var publicationUpdate = addAnotherUnpublishedFile(publishedPublication);

        WireMock.reset();

        stubSuccessfulTokenResponse();
        stubCustomerResponseNotFound(customerId);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(problem.getDetail(), is(equalTo("Customer API not responding or not responding as expected!")));
    }

    @Test
    void shouldUpdatePublicationWhenUserIsCuratorAndIsInSameInstitutionAsThePublicationContributor()
        throws BadRequestException, IOException, NotFoundException {
        Publication savedPublication = createSamplePublication();

        injectRandomContributorsWithoutCristinIdAndIdentity(savedPublication);
        var cristinId = randomUri();
        var contributor = createContributorForPublicationUpdate(cristinId);
        injectContributor(savedPublication, contributor);
        var customerId = ((Organization) contributor.getAffiliations().get(0)).getId();

        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream event = curatorUpdatesPublicationAndHasRightToUpdate(publicationUpdate, customerId);
        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Publication.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = publicationService.getPublicationByIdentifier(savedPublication.getIdentifier());

        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        var expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        var actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @Test
    void shouldUpdateNonDegreePublicationWhenUserHasAccessRightEditAllNonDegreePublications()
        throws ApiGatewayException, IOException {
        var savedPublication =  persistPublication(createNonDegreePublication().copy()).build();
        var publicationUpdate = updateTitle(savedPublication);
        var event = userWithEditAllNonDegreePublicationsUpdatesPublication(customerId, publicationUpdate);

        updatePublicationHandler.handleRequest(event, output, context);

        var response = GatewayResponse.fromOutputStream(output, Publication.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_OK)));

        var updatedPublication = publicationService.getPublicationByIdentifier(savedPublication.getIdentifier());

        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        var expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        var actualTitle = updatedPublication.getEntityDescription().getMainTitle();

        assertThat(actualTitle, is(equalTo(expectedTitle)));
        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @ParameterizedTest(name = "Should update degree publication when user has access rights to edit degree")
    @MethodSource("allDegreeInstances")
    void shouldUpdateDegreePublicationWhenUserHasAccessRightToEditDegree(Class<?> degree)
        throws BadRequestException, IOException, NotFoundException {
        Publication degreePublication = savePublication(randomPublicationWithPublisher(customerId, degree));
        Publication publicationUpdate = updateTitle(degreePublication);
        InputStream event = userWithAccessRightToEditDegree(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);
        Publication updatedPublication =
            publicationService.getPublicationByIdentifier(degreePublication.getIdentifier());

        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        String expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        String actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @ParameterizedTest(name = "Should update degree publication when user is resource owner")
    @MethodSource("allDegreeInstances")
    void shouldUpdateDegreePublicationWhenUserIsResourceOwner(Class<?> degree)
        throws BadRequestException, IOException, NotFoundException {
        Publication degreePublication = savePublication(randomPublicationWithPublisher(customerId, degree));
        Publication publicationUpdate = updateTitle(degreePublication);
        InputStream event = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);
        Publication updatedPublication =
            publicationService.getPublicationByIdentifier(degreePublication.getIdentifier());

        publicationUpdate.setModifiedDate(updatedPublication.getModifiedDate());

        String expectedTitle = publicationUpdate.getEntityDescription().getMainTitle();
        String actualTitle = updatedPublication.getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
        assertThat(updatedPublication, is(equalTo(publicationUpdate)));
    }

    @ParameterizedTest(name = "Should return Unauthorized publication when user does not has access rights to edit "
                              + "degree and is not publication owner and the publication is Degree")
    @MethodSource("allDegreeInstances")
    void shouldReturnForbiddenWhenUserDoesNotHasAccessRightToEditDegree(Class<?> degree)
        throws BadRequestException, IOException {
        Publication degreePublication = savePublication(randomPublicationWithPublisher(customerId, degree));
        Publication publicationUpdate = updateTitle(degreePublication);
        InputStream event = userWithEditAllNonDegreePublicationsUpdatesPublication(customerId, publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void handlerUpdatesDegreePublicationWhenInputIsValidAndUserIsExternalClient()
        throws IOException, BadRequestException {

        var thesisPublication = publication.copy().withEntityDescription(thesisPublishableEntityDescription()).build();
        var savedThesis = Resource
                              .fromPublication(thesisPublication)
                              .persistNew(publicationService, UserInstance.fromPublication(publication));
        var publicationUpdate = updateTitle(savedThesis);

        InputStream inputStream =
            externalClientUpdatesPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        when(getExternalClientResponse.getCustomerUri())
            .thenReturn(publication.getPublisher().getId());
        when(getExternalClientResponse.getActingUser())
            .thenReturn(publication.getResourceOwner().getOwner().getValue());

        updatePublicationHandler.handleRequest(inputStream, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, PublicationResponse.class);
        final PublicationResponse body = gatewayResponse.getBodyObject(PublicationResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        assertThat(body.getEntityDescription().getMainTitle(),
                   is(equalTo(publicationUpdate.getEntityDescription().getMainTitle())));
    }

    @Test
    void shouldUpdateExistingPendingPublishingRequestWhenPublicationUpdateRemovesFiles()
        throws IOException, ApiGatewayException {
        var persistedPublication = persistPublication(createNonDegreePublication().copy()).build();
        publish(persistedPublication);
        var existingTicket = TicketEntry.requestNewTicket(persistedPublication, PublishingRequestCase.class)
                                 .persistNewTicket(ticketService);
        var updatedPublication = persistedPublication.copy().withAssociatedArtifacts(List.of()).build();
        var input = ownerUpdatesOwnPublication(updatedPublication.getIdentifier(), updatedPublication);
        updatePublicationHandler.handleRequest(input, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(existingTicket.fetch(ticketService).getStatus(), is(equalTo(TicketStatus.COMPLETED)));
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HTTP_OK)));
    }

    @Test
    void shouldUpdateFilesForApprovalWhenPublicationUpdateHasFileChanges() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithUnpublishedFiles(
            customerId, PublicationStatus.PUBLISHED, publicationService);
        var ticket = TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService);
        var expectedFilesForApprovalBeforePublicationUpdate = getUnpublishedFiles(publication);

        assertThat(((PublishingRequestCase) ticket).getFilesForApproval(),
                   containsInAnyOrder(expectedFilesForApprovalBeforePublicationUpdate.toArray()));

        var newUnpublishedFile = File.builder().withIdentifier(UUID.randomUUID()).buildUnpublishedFile();
        updatePublicationWithFile(publication, newUnpublishedFile);

        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);
        updatePublicationHandler.handleRequest(input, output, context);

        var filesForApproval = fetchFilesForApprovalFromPendingPublishingRequest(publication);

        var expectedFilesForApproval = mergeExistingFilesForApprovalWithNewFile(
            expectedFilesForApprovalBeforePublicationUpdate, newUnpublishedFile);

        assertThat(filesForApproval, containsInAnyOrder(expectedFilesForApproval.toArray()));
    }

    @Test
    void shouldUpdatePublicationWithoutReferencedContext()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithAdministrativeAgreement(customerId,
                                                                                                publicationService);
        publication.getEntityDescription().getReference().setDoi(null);
        publicationService.updatePublication(publication);
        TicketTestUtils.createPersistedTicket(publication, PublishingRequestCase.class, ticketService)
            .complete(publication, new Username(randomString())).persistUpdate(ticketService);

        var newUnpublishedFile = File.builder().withIdentifier(UUID.randomUUID())
                                     .withLicense(randomUri()).buildUnpublishedFile();
        var files = publication.getAssociatedArtifacts();
        files.add(newUnpublishedFile);

        publication.copy().withAssociatedArtifacts(files);

        var input = ownerUpdatesOwnPublication(publication.getIdentifier(), publication);
        updatePublicationHandler.handleRequest(input, output, context);

        assertThat(GatewayResponse.fromOutputStream(output, Void.class).getStatusCode(), is(equalTo(200)));
    }

    @Test
    void shouldRejectUpdateIfSettingInstanceTypeNotAllowingFilesOnPublicationContainingFile()
        throws BadRequestException, IOException {

        WireMock.reset();

        stubSuccessfulTokenResponse();
        stubSuccessfulCustomerResponseAllowingFilesForNoTypes(customerId);

        var savedPublication = createSamplePublication();
        var publicationUpdate = updateTitle(savedPublication);

        var event = userUpdatesPublicationAndHasRightToUpdate(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));

        var problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), is(startsWith("Files not allowed for instance type ")));
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingAccessRightsWhenUnpublishingPublication()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistPublicationWithoutDoi(true);

        var unpublishRequest = new UnpublishPublicationRequest();
        unpublishRequest.setComment("comment");

        var inputStream = new HandlerRequestBuilder<UnpublishPublicationRequest>(restApiMapper)
                              .withUserName(randomString())
                              .withCurrentCustomer(RandomPersonServiceResponse.randomUri())
                              .withAccessRights(null)
                              .withBody(unpublishRequest)
                              .withPathParameters(
                                  Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                              .build();

        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotPublicationOwnerWhenUnpublishingPublication() throws IOException,
                                                                                                   ApiGatewayException {
        var publication = createAndPersistPublicationWithoutDoi(true);

        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), randomString(),
                                                        RandomPersonServiceResponse.randomUri(),
                                                        randomUri(),
                                                        USER);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotContributorWhenUnpublishingPublication()
        throws IOException, ApiGatewayException {

        var userId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();

        var publication = createPublicationWithoutDoiAndWithContributor(userId, userName);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), randomString(),
                                                        RandomPersonServiceResponse.randomUri(),
                                                        RandomPersonServiceResponse.randomUri(), USER);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldSuccessWhenUserIsContributorWhenUnpublishingPublication()
        throws ApiGatewayException, IOException {

        var userCristinId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();

        var publication = createPublicationWithoutDoiAndWithContributor(userCristinId, userName);

        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), userName,
                                                        RandomPersonServiceResponse.randomUri(), userCristinId, USER);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldProduceUpdateDoiEventWhenUnpublishingIsSuccessful()
        throws ApiGatewayException, IOException {

        var userCristinId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();
        var doi = RandomPersonServiceResponse.randomUri();

        var publication = createPublicationWithContributorAndDoi(userCristinId, userName, doi);

        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), userName,
                                                        RandomPersonServiceResponse.randomUri(), userCristinId, USER);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        assertTrue(eventBridgeClient.getRequestEntries()
                       .stream()
                       .anyMatch(entry -> entry.source().equals(NVA_PUBLICATION_DELETE_SOURCE)
                                          && entry.detailType().equals(LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS)));
    }

    @Test
    void shouldProduceUpdateDoiEventWithDuplicateWhenUnpublishing()
        throws ApiGatewayException, IOException {

        var userCristinId = RandomPersonServiceResponse.randomUri();
        var userName = randomString();
        var doi = RandomPersonServiceResponse.randomUri();
        var duplicate = SortableIdentifier.next();

        var publication = createPublicationWithOwnerAndDoi(userCristinId, userName, doi);

        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createRequestWithDuplicateOfValue(publication.getIdentifier(), userName,
                                                            RandomPersonServiceResponse.randomUri(),
                                                            duplicate, USER);

        updatePublicationHandler.handleRequest(inputStream, output, context);

        assertThat(eventBridgeClient.getRequestEntries()
                       .stream()
                       .filter(a -> a.source().equals(NVA_PUBLICATION_DELETE_SOURCE))
                       .map(UpdatePublicationHandlerTest::getDoiMetadataUpdateEvent)
                       .map(LambdaDestinationInvocationDetail::responsePayload)
                       .filter(a -> nonNull(a.getDuplicateOf()))
                       .findFirst()
                       .orElseThrow()
                       .getDuplicateOf(),
                   Is.is(IsEqual.equalTo(toPublicationUri(duplicate.toString()))));
    }

    @Test
    void shouldReturnSuccessWhenUserIsResourceOwnerWhenUnpublishingPublication()
        throws ApiGatewayException, IOException {

        var userName = randomString();
        var institutionId = RandomPersonServiceResponse.randomUri();

        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(userName, institutionId);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), userName, institutionId, USER);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnBadRequestWhenUnpublishingNotPublishedPublication() throws IOException, ApiGatewayException {
        var unpublishedPublication = createAndPersistPublicationWithoutDoi(false);
        var publisherUri = unpublishedPublication.getPublisher().getId();

        var inputStream = createUnpublishHandlerRequest(unpublishedPublication.getIdentifier(), randomString(),
                                                        publisherUri,
                                                        AccessRight.MANAGE_RESOURCES_ALL);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_BAD_REQUEST)));
    }

    // TODO: Should this return 200 OK?
    @Test
    void shouldReturnNotFoundWhenPublicationDoesNotExist() throws IOException {
        var inputStream = createUnpublishHandlerRequest(SortableIdentifier.next(), randomString(),
                                                        RandomPersonServiceResponse.randomUri(),
                                                        USER);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWhenDeletingUnsupportedPublicationStatus() throws BadRequestException, IOException {
        var publication = randomPublication().copy().withStatus(PublicationStatus.NEW).build();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(publicationService, UserInstance.fromPublication(publication));

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(persistedPublication.getIdentifier(), randomString(),
                                                        publisherUri,
                                                        AccessRight.MANAGE_RESOURCES_ALL);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_BAD_REQUEST)));
    }

    @Test
    void shouldReturnSuccessAndUpdatePublicationStatusToDeletedWhenUserCanEditOwnInstitutionResources()
        throws IOException, ApiGatewayException {

        var publication = createAndPersistPublicationWithoutDoi(true);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), randomString(), publisherUri,
                                                        AccessRight.MANAGE_RESOURCES_STANDARD);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));

        var unpublishedPublication = publicationService.getPublication(publication);
        assertThat(unpublishedPublication.getStatus(), Is.is(IsEqual.equalTo(PublicationStatus.UNPUBLISHED)));
    }

    @Test
    void shouldReturnSuccessWhenEditorUnpublishesDegree() throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), randomString(), publisherUri,
                                                        MANAGE_DEGREE, MANAGE_RESOURCES_ALL);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenNonEditorUnpublishesDegree() throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), randomString(), publisherUri,
                                                        USER);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void curatorShouldUpdateDeletedResourceWithDuplicateOfValueWhenResourceIsADuplicate()
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var duplicate = SortableIdentifier.next();
        var request = createRequestWithDuplicateOfValue(publication.getIdentifier(),
                                                        randomString(),
                                                        publication.getPublisher().getId(),
                                                        duplicate,
                                                        MANAGE_DEGREE, MANAGE_RESOURCES_ALL);
        updatePublicationHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var updatedPublication = publicationService.getPublication(publication);
        String duplicateIdentifier = UriWrapper.fromUri(updatedPublication.getDuplicateOf()).getLastPathElement();

        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
        assertThat(duplicateIdentifier, Is.is(IsEqual.equalTo(duplicate.toString())));
    }

    @Test
    void shouldReturnSuccessWhenCuratorUnpublishesPublishedPublicationFromOwnInstitution()
        throws ApiGatewayException, IOException {

        var institutionId = RandomPersonServiceResponse.randomUri();
        var curatorUsername = randomString();
        var curatorAccessRight = AccessRight.MANAGE_RESOURCES_STANDARD;
        var resourceOwnerUsername = randomString();

        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(resourceOwnerUsername,
                                                                                    institutionId);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), curatorUsername, institutionId,
                                                        curatorAccessRight);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenCuratorUnpublishesPublishedPublicationFromAnotherInstitution()
        throws IOException, ApiGatewayException {

        var curatorUsername = randomString();
        var curatorInstitutionId = RandomPersonServiceResponse.randomUri();

        var resourceOwnerUsername = randomString();
        var resourceOwnerInstitutionId = RandomPersonServiceResponse.randomUri();

        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(resourceOwnerUsername,
                                                                                    resourceOwnerInstitutionId);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createUnpublishHandlerRequest(publication.getIdentifier(), curatorUsername,
                                                        curatorInstitutionId,
                                                        AccessRight.MANAGE_RESOURCES_STANDARD);
        updatePublicationHandler.handleRequest(inputStream, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldPersistUnpublishRequestWhenDeletingPublishedPublication()
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var publisherUri = publication.getPublisher().getId();
        var request = createUnpublishHandlerRequest(publication.getIdentifier(), randomString(), publisherUri,
                                                    MANAGE_DEGREE, MANAGE_RESOURCES_STANDARD);
        updatePublicationHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var persistedTicket = ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                                            publication.getIdentifier(),
                                                                            UnpublishRequest.class);

        assertTrue(persistedTicket.isPresent());
        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldDeleteUnpublishedPublicationWhenUserIsEditor()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublication();

        var publisherUri = publication.getPublisher().getId();
        var request = creatDeleteHandlerRequest(publication.getIdentifier(), randomString(),
                                                publisherUri, null, MANAGE_RESOURCES_ALL,
                                                MANAGE_DEGREE);// MANAGE_DEGREE here?
        updatePublicationHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        var deletePublication = publicationService.getPublication(publication);

        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_ACCEPTED)));
        assertThat(deletePublication.getStatus(), Is.is(IsEqual.equalTo(PublicationStatus.DELETED)));
        assertThat(deletePublication.getAssociatedArtifacts(), Is.is(emptyIterable()));
        publication.getAssociatedArtifacts().stream().filter(File.class::isInstance).map(File.class::cast).forEach(
            file -> verify(s3Client).deleteObject(DeleteObjectRequest.builder()
                                                      .bucket(NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY)
                                                      .key(file.getIdentifier().toString())
                                                      .build()));
    }

    @Test
    void ownerUserShouldGetUnauthorizedWhenHardDeleteFiles()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublication();

        var publisherUri = publication.getPublisher().getId();
        var request = creatDeleteHandlerRequest(publication.getIdentifier(),
                                                publication.getResourceOwner().getOwner().getValue(),
                                                publisherUri, null, USER);
        updatePublicationHandler.handleRequest(request, output, context);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(SC_UNAUTHORIZED)));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    private Publication createUnpublishedPublication() throws ApiGatewayException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        publicationService.unpublishPublication(publicationService.getPublication(publication));
        return publicationService.getPublication(publication);
    }

    private InputStream createUnpublishHandlerRequest(SortableIdentifier publicationIdentifier, String username,
                                                      URI institutionId, AccessRight... accessRight)
        throws JsonProcessingException {

        return createUnpublishHandlerRequest(publicationIdentifier, username, institutionId, null, accessRight);
    }

    private InputStream createUnpublishHandlerRequest(SortableIdentifier publicationIdentifier, String username,
                                                      URI institutionId, URI cristinId, AccessRight... accessRight)
        throws JsonProcessingException {
        var unpublishRequest = new UnpublishPublicationRequest();
        unpublishRequest.setComment("comment");
        var request = new HandlerRequestBuilder<UnpublishPublicationRequest>(restApiMapper)
                          .withUserName(username)
                          .withCurrentCustomer(institutionId)
                          .withAccessRights(institutionId, accessRight)
                          .withBody(unpublishRequest)
                          .withPathParameters(
                              Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString()));

        if (nonNull(cristinId)) {
            request.withPersonCristinId(cristinId);
        }

        return request.build();
    }

    private InputStream creatDeleteHandlerRequest(SortableIdentifier publicationIdentifier, String username,
                                                  URI institutionId, URI cristinId, AccessRight... accessRight)
        throws JsonProcessingException {
        var request = new HandlerRequestBuilder<DeletePublicationRequest>(restApiMapper)
                          .withUserName(username)
                          .withCurrentCustomer(institutionId)
                          .withAccessRights(institutionId, accessRight)
                          .withBody(new DeletePublicationRequest())
                          .withPathParameters(
                              Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString()));

        if (nonNull(cristinId)) {
            request.withPersonCristinId(cristinId);
        }

        return request.build();
    }

    private InputStream createRequestWithDuplicateOfValue(SortableIdentifier publicationIdentifier, String username,
                                                          URI institutionId,
                                                          SortableIdentifier duplicateOf,
                                                          AccessRight... accessRight)
        throws JsonProcessingException {
        var unpublishRequest = new UnpublishPublicationRequest();
        unpublishRequest.setComment("comment");
        unpublishRequest.setDuplicateOf(duplicateOf);
        var request = new HandlerRequestBuilder<UnpublishPublicationRequest>(restApiMapper)
                          .withUserName(username)
                          .withCurrentCustomer(institutionId)
                          .withAccessRights(institutionId, accessRight)
                          .withBody(unpublishRequest)
                          .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString()));

        return request.build();
    }

    private Publication createAndPersistPublicationWithoutDoi(boolean shouldBePublished) throws ApiGatewayException {
        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withDoi(null).build();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(publicationService, UserInstance.fromPublication(publication));

        if (shouldBePublished) {
            publicationService.publishPublication(UserInstance.fromPublication(persistedPublication),
                                                  persistedPublication.getIdentifier());
        }

        return persistedPublication;
    }

    private Publication createPublicationWithContributorAndDoi(URI contributorId, String contributorName,
                                                               URI doi)
        throws ApiGatewayException {

        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withDoi(doi).build();

        var identity = new Identity.Builder().withName(contributorName).withId(contributorId).build();
        var contributor = new Contributor.Builder().withIdentity(identity).withRole(new RoleType(Role.CREATOR)).build();
        var entityDesc = publication.getEntityDescription().copy().withContributors(List.of(contributor)).build();
        var publicationWithContributor = publication.copy().withEntityDescription(entityDesc).build();

        return Resource.fromPublication(publicationWithContributor)
                   .persistNew(publicationService, UserInstance.fromPublication(publication));
    }

    private Publication createPublicationWithOwnerAndDoi(URI contributorId, String owner,
                                                         URI doi)
        throws ApiGatewayException {
        var identity = new Identity.Builder().withName(owner).withId(contributorId).build();
        var contributor = new Contributor.Builder().withIdentity(identity).withRole(new RoleType(Role.CREATOR)).build();
        var entityDescription = randomEntityDescription(JournalArticle.class).copy()
                                    .withContributors(List.of(contributor))
                                    .build();

        var publication = randomPublication().copy()
                              .withEntityDescription(entityDescription)
                              .withDuplicateOf(null)
                              .withResourceOwner(
                                  new ResourceOwner(new Username(owner), RandomPersonServiceResponse.randomUri()))
                              .withDoi(doi).build();

        return Resource.fromPublication(publication)
                   .persistNew(publicationService, UserInstance.fromPublication(publication));
    }

    private Publication createPublicationWithoutDoiAndWithContributor(URI contributorId, String contributorName)
        throws ApiGatewayException {

        return createPublicationWithContributorAndDoi(contributorId, contributorName, null);
    }

    private Publication createAndPersistDegreeWithoutDoi() throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).build();

        var degreePhd = new DegreePhd(new MonographPages(), new PublicationDate(),
                                      Set.of(new UnconfirmedDocument(randomString())));
        var reference = new Reference.Builder().withPublicationInstance(degreePhd).build();
        var entityDescription = publication.getEntityDescription().copy().withReference(reference).build();
        var publicationOfTypeDegree = publication.copy().withEntityDescription(entityDescription).build();

        return Resource.fromPublication(publicationOfTypeDegree)
                   .persistNew(publicationService, UserInstance.fromPublication(publication));
    }

    private Publication createAndPersistPublicationWithoutDoiAndWithResourceOwner(String userName, URI institution)
        throws BadRequestException {

        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withDoi(null)
                              .withResourceOwner(new ResourceOwner(new Username(userName), institution))
                              .withPublisher(new Organization.Builder().withId(institution).build())
                              .build();

        return Resource.fromPublication(publication)
                   .persistNew(publicationService, UserInstance.fromPublication(publication));
    }

    private static URI toPublicationUri(String identifier) {
        return UriWrapper.fromHost(new Environment().readEnv(API_HOST))
                   .addChild(PUBLICATION)
                   .addChild(identifier)
                   .getUri();
    }

    private static LambdaDestinationInvocationDetail<DoiMetadataUpdateEvent> getDoiMetadataUpdateEvent(
        PutEventsRequestEntry a) {
        try {
            return JsonUtils.dtoObjectMapper.readValue(a.detail(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<FileForApproval> fetchFilesForApprovalFromPendingPublishingRequest(Publication publication) {
        return ticketService.fetchTicketsForUser(UserInstance.fromPublication(publication))
                   .filter(PublishingRequestCase.class::isInstance)
                   .filter(ticketEntry -> PENDING.equals(ticketEntry.getStatus()))
                   .map(PublishingRequestCase.class::cast)
                   .map(PublishingRequestCase::getFilesForApproval)
                   .collect(SingletonCollector.collect());
    }

    private List<FileForApproval> mergeExistingFilesForApprovalWithNewFile(List<FileForApproval> list, File file) {
        list = new ArrayList<>(list);
        list.add(FileForApproval.fromFile(file));
        return list;
    }

    private void updatePublicationWithFile(Publication publication, File newUnpublishedFile) {
        AssociatedArtifactList associatedArtifacts = publication.getAssociatedArtifacts();
        associatedArtifacts.add(newUnpublishedFile);
        publication.setAssociatedArtifacts(associatedArtifacts);
        publicationService.updatePublication(publication);
    }

    private static List<FileForApproval> getUnpublishedFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(UnpublishedFile.class::isInstance)
                   .map(File.class::cast)
                   .map(FileForApproval::fromFile).toList();
    }

    private void publish(Publication persistedPublication) throws ApiGatewayException {
        publicationService.publishPublication(UserInstance.fromPublication(publication),
                                              persistedPublication.getIdentifier());
    }

    private Publication savePublication(Publication degreePublication) throws BadRequestException {
        UserInstance userInstance = UserInstance.fromPublication(degreePublication);
        return Resource.fromPublication(degreePublication).persistNew(publicationService, userInstance);
    }

    private void injectRandomContributorsWithoutCristinIdAndIdentity(Publication publication) {
        var contributorWithoutCristinId = new Contributor.Builder()
                                              .withRole(new RoleType(Role.ARCHITECT))
                                              .withIdentity(new Identity.Builder().withName(randomString()).build())
                                              .build();
        var contributorWithoutIdentity = new Contributor.Builder()
                                             .withRole(new RoleType(Role.ARCHITECT))
                                             .build();
        var contributors = new ArrayList<>(publication.getEntityDescription().getContributors());
        contributors.addAll(List.of(contributorWithoutCristinId, contributorWithoutIdentity));
        publication.getEntityDescription().setContributors(contributors);
    }

    private void injectContributor(Publication savedPublication, Contributor contributor) {
        var contributors = new ArrayList<>(savedPublication.getEntityDescription().getContributors());
        contributors.add(contributor);
        savedPublication.getEntityDescription().setContributors(contributors);
        publicationService.updatePublication(savedPublication);
    }

    private Contributor createContributorForPublicationUpdate(URI cristinId) {
        return new Contributor.Builder()
                   .withRole(new RoleType(Role.ARCHITECT))
                   .withIdentity(new Identity.Builder().withId(cristinId).withName(randomString()).build())
                   .withAffiliations(getListOfRandomOrganizations())
                   .build();
    }

    private List<Corporation> getListOfRandomOrganizations() {
        return List.of(new Organization.Builder().withId(RandomPersonServiceResponse.randomUri()).build());
    }

    private boolean containsOneCompletedAndOnePendingPublishingRequest(List<TicketEntry> tickets) {
        var statuses = tickets.stream().map(TicketEntry::getStatus).toList();
        return statuses.stream().anyMatch(TicketStatus.COMPLETED::equals)
               && statuses.stream().anyMatch(TicketStatus.PENDING::equals);
    }

    private TicketEntry createPendingPublishingRequest(Publication publishedPublication) throws ApiGatewayException {
        return PublishingRequestCase.createNewTicket(publishedPublication, PublishingRequestCase.class,
                                                     SortableIdentifier::next).persistNewTicket(ticketService);
    }

    private TicketEntry persistCompletedPublishingRequest(Publication publishedPublication) throws ApiGatewayException {
        var ticket = PublishingRequestCase.createNewTicket(publishedPublication, PublishingRequestCase.class,
                                                           SortableIdentifier::next).persistNewTicket(ticketService);
        return ticketService.updateTicketStatus(ticket, TicketStatus.COMPLETED, new Username(randomString()));
    }

    private Publication createSamplePublication() throws BadRequestException {
        UserInstance userInstance = UserInstance.fromPublication(publication);
        return Resource.fromPublication(publication).persistNew(publicationService, userInstance);
    }

    private InputStream requestWithoutUsername(Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(randomUri())
                   .withBody(publicationUpdate)
                   .build();
    }

    private InputStream userWithAccessRightToEditDegree(Publication publicationUpdate) throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_DEGREE, MANAGE_RESOURCES_ALL)
                   .build();
    }

    private InputStream userWithEditAllNonDegreePublicationsUpdatesPublication(URI customerId,
                                                                               Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_RESOURCES_ALL)
                   .withUserName(SOME_CURATOR)
                   .build();
    }

    private InputStream userUpdatesPublicationOfOtherInstitution(Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        URI customerId = randomUri();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD)
                   .withUserName(SOME_CURATOR)
                   .build();
    }

    private InputStream contributorUpdatesPublicationAndHasRightsToUpdate(Publication publicationUpdate,
                                                                          URI cristinId)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withPersonCristinId(cristinId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD)
                   .build();
    }

    private InputStream contributorUpdatesPublicationWithoutHavingRights(Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .build();
    }

    private InputStream userUpdatesPublicationAndHasRightToUpdate(Publication publicationUpdate)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withPersonCristinId(randomUri())
                   .withAccessRights(customerId, MANAGE_RESOURCES_STANDARD, MANAGE_DEGREE)
                   .build();
    }

    private InputStream ownerUpdatesOwnPublication(SortableIdentifier publicationIdentifier,
                                                   Publication publicationUpdate) throws JsonProcessingException {
        return ownerUpdatesOwnPublication(publicationIdentifier, publicationUpdate, null);
    }

    private InputStream ownerUpdatesOwnPublication(SortableIdentifier publicationIdentifier,
                                                   Publication publicationUpdate,
                                                   URI cristinId)
        throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString());

        var customerId = publicationUpdate.getPublisher().getId();
        var request = new HandlerRequestBuilder<Publication>(restApiMapper)
                          .withUserName(publicationUpdate.getResourceOwner().getOwner().getValue())
                          .withCurrentCustomer(customerId)
                          .withBody(publicationUpdate)
                          .withPathParameters(pathParameters);

        if (nonNull(cristinId)) {
            request.withPersonCristinId(cristinId);
        }

        return request.build();
    }

    private InputStream externalClientUpdatesPublication(SortableIdentifier publicationIdentifier,
                                                         Publication publicationUpdate)
        throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString());

        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                   .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                   .withBody(publicationUpdate)
                   .withPathParameters(pathParameters)
                   .build();
    }

    private Publication updateTitle(Publication savedPublication) {
        Publication update = savedPublication.copy().build();
        update.getEntityDescription().setMainTitle(randomString());
        return update;
    }

    private Publication addAnotherUnpublishedFile(Publication savedPublication) {
        Publication update = savedPublication.copy().build();
        var associatedArtifacts = update.getAssociatedArtifacts();
        associatedArtifacts.add(randomFile());
        update.setAssociatedArtifacts(associatedArtifacts);
        return update;
    }

    private AssociatedArtifact randomFile() {
        return new UnpublishedFile(UUID.randomUUID(), randomString(), randomString(),
                                   Long.valueOf(randomInteger().toString()),
                                   new License.Builder().withIdentifier(randomString()).withLink(randomUri()).build(),
                                   false, false, null, null, randomString());
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
                   .withBody(createNonDegreePublication())
                   .withHeaders(generateHeaders());
    }

    private Map<String, String> generateHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return headers;
    }

    private Publication createNonDegreePublication() {
        var publicationInstanceTypes = listPublicationInstanceTypes();
        var nonDegreePublicationInstances = publicationInstanceTypes.stream()
                                                .filter(this::isNonDegreeClass)
                                                .collect(Collectors.toList());
        var publication = randomPublication(randomElement(nonDegreePublicationInstances));
        publication.setIdentifier(SortableIdentifier.next());
        return publication;
    }

    private boolean isNonDegreeClass(Class<?> publicationInstance) {
        var listOfDegreeClasses = Set.of("DegreeMaster", "DegreeBachelor", "DegreePhd");
        return !listOfDegreeClasses.contains(publicationInstance.getSimpleName());
    }

    private GatewayResponse<Problem> toGatewayResponseProblem() throws JsonProcessingException {
        return restApiMapper.readValue(output.toString(),
                                       PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE);
    }

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    private EntityDescription thesisPublishableEntityDescription() {
        return new EntityDescription.Builder()
                   .withMainTitle(RandomDataGenerator.randomString())
                   .withReference(
                       new Reference.Builder()
                           .withDoi(RandomDataGenerator.randomDoi())
                           .withPublicationInstance(
                               PublicationInstanceBuilder.randomPublicationInstance(DegreeMaster.class))
                           .build())
                   .build();
    }

    private InputStream curatorUpdatesPublicationAndHasRightToUpdate(Publication publicationUpdate, URI customerId)
        throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                   .withUserName(SOME_CURATOR)
                   .withPathParameters(pathParameters)
                   .withCurrentCustomer(customerId)
                   .withBody(publicationUpdate)
                   .withAccessRights(customerId, MANAGE_DOI, MANAGE_RESOURCES_STANDARD)
                   .build();
    }
}
