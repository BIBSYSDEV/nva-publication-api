package no.unit.nva.publication.update;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.License;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.http.entity.ContentType;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.RequestUtil.IDENTIFIER_IS_NOT_A_VALID_UUID;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static no.unit.nva.publication.update.UpdatePublicationHandler.UNABLE_TO_FETCH_CUSTOMER_ERROR_MESSAGE;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.PUBLISH_THESIS;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdatePublicationHandlerTest extends ResourcesLocalTest {

    public static final JavaType PARAMETERIZED_GATEWAY_RESPONSE_PROBLEM_TYPE =
            restApiMapper.getTypeFactory().constructParametricType(GatewayResponse.class, Problem.class);
    public static final String ACCESS_TOKEN_RESPONSE_BODY = "{ \"access_token\" : \"Bearer token\"}";

    public static final String SOME_MESSAGE = "SomeMessage";
    public static final String SOME_CURATOR = "some@curator";
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    private final GetExternalClientResponse getExternalClientResponse = mock(GetExternalClientResponse.class);
    private ResourceService publicationService;
    private Context context;
    private ByteArrayOutputStream output;
    private UpdatePublicationHandler updatePublicationHandler;
    private Publication publication;
    private Environment environment;
    private IdentityServiceClient identityServiceClient;
    private TicketService ticketService;
    private FakeSecretsManagerClient secretsManagerClient;

    private static FakeHttpClient<String> getHttpClientWithPublisherAllowingPublishing() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK),
                mockIdentityServiceResponseAllowingAutoApprovalOfPublishingRequests());
    }

    private static FakeHttpResponse<String> mockIdentityServiceResponseAllowingAutoApprovalOfPublishingRequests() {
        return FakeHttpResponse.create(IoUtils.stringFromResources(Path.of("customer_allowing_publishing.json")),
                HTTP_OK);
    }

    private static FakeHttpClient<String> getHttpClientWithUnresolvableClient() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY,
                HTTP_OK), unresolvableCustomer());
    }

    private static FakeHttpResponse<String> unresolvableCustomer() {
        return FakeHttpResponse.create(randomString(), HTTP_NOT_FOUND);
    }

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() throws NotFoundException {
        super.init();

        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");

        publicationService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
        context = mock(Context.class);

        identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);

        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("secret", credentials.toString());
        output = new ByteArrayOutputStream();
        var uriRetriever = getUriRetriever(getHttpClientWithPublisherAllowingPublishing(), secretsManagerClient);
        updatePublicationHandler =
                new UpdatePublicationHandler(publicationService, ticketService, environment, identityServiceClient,
                        uriRetriever);
        publication = createPublication();
    }

    @Test
    void handlerUpdatesPublicationWhenInputIsValidAndUserIsResourceOwner()
            throws IOException, ApiGatewayException {
        publication = PublicationGenerator.publicationWithoutIdentifier();
        Publication savedPublication = createSamplePublication();

        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);

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
    void handlerCreatesPendingPublishingRequestTicketForPublishedPublicationWhenUpdatingFiles()
            throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED,
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
        assertThat(ticket.map(PublishingRequestCase::getStatus).orElseThrow(), is(equalTo(TicketStatus.PENDING)));
    }

    @Test
    void handlerCreatesPendingPublishingRequestTicketForPublishedPublicationWhenCompletedPublishingRequestExists()
            throws ApiGatewayException, IOException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED,
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
        var publishedPublication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED,
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
        var publishedPublication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED,
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
        publication = PublicationGenerator.publicationWithoutIdentifier();
        Publication savedPublication = createSamplePublication();

        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream inputStream =
                externalClientUpdatesPublication(publicationUpdate.getIdentifier(), publicationUpdate);

        when(getExternalClientResponse.getCustomerUri()).thenReturn(publication.getPublisher().getId());
        when(getExternalClientResponse.getActingUser()).thenReturn(publication.getResourceOwner().getOwner().getValue());

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
        updatePublicationHandler = new UpdatePublicationHandler(publicationService, ticketService, environment,
                identityServiceClient,
                getUriRetriever(
                        getHttpClientWithPublisherAllowingPublishing(),
                        secretsManagerClient));

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
        updatePublicationHandler = new UpdatePublicationHandler(publicationService, ticketService, environment,
                identityServiceClient, getUriRetriever(
                getHttpClientWithPublisherAllowingPublishing(),
                secretsManagerClient));
        Publication savedPublication = createSamplePublication();

        InputStream event = ownerUpdatesOwnPublication(savedPublication.getIdentifier(), savedPublication);
        updatePublicationHandler.handleRequest(event, output, context);
        GatewayResponse<Problem> gatewayResponse = toGatewayResponseProblem();
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(appender.getMessages(), containsString(SOME_MESSAGE));
    }

    @Test
    void handlerReturnsBadRequestWhenIdentifierInPathDiffersFromIdentifierInBody() throws IOException {

        SortableIdentifier someOtherIdentifier = SortableIdentifier.next();

        InputStream event = ownerUpdatesOwnPublication(someOtherIdentifier, publication);

        updatePublicationHandler.handleRequest(event, output, context);
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
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
            throws ApiGatewayException, IOException {
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
        Problem problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserCannotBeIdentified() throws IOException {
        var event = requestWithoutUsername(randomPublication());
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

    private void injectRandomContributorsWithoutCristinIdAndIdentity(Publication publication) {
        var contributorWithoutCristinId = new Contributor.Builder()
                .withRole(new RoleType(Role.ARCHITECT))
                .withIdentity(new Identity.Builder().withName(randomString()).build())
                .build();
        var contributorWithoutIdentity = new Contributor.Builder()
                .withRole(new RoleType(Role.ARCHITECT))
                .build();
        publication.getEntityDescription().getContributors()
                .addAll(List.of(contributorWithoutCristinId, contributorWithoutIdentity));
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
    void
    shouldReturnForbiddenWhenContributorWithoutCristinIdUpdatesResource() throws BadRequestException, IOException {
        Publication savedPublication = createSamplePublication();
        var contributor = createContributorForPublicationUpdate(null);
        injectContributor(savedPublication, contributor);
        Publication publicationUpdate = updateTitle(savedPublication);

        InputStream event = contributorUpdatesPublicationWithoutHavingRights(publicationUpdate);
        updatePublicationHandler.handleRequest(event, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }

    @Test
    void shouldReturnForbiddenWhenContributorUpdatesResourceWithoutEntityDescription() throws BadRequestException, IOException {
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
    void shouldReturnBadGatewayWhenHttpClientUnableToRetrievePublishingWorkflow()
            throws IOException, ApiGatewayException {
        var publishedPublication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED,
                publicationService);

        var publicationUpdate = addAnotherUnpublishedFile(publishedPublication);

        var inputStream = ownerUpdatesOwnPublication(publicationUpdate.getIdentifier(), publicationUpdate);
        this.updatePublicationHandler = new UpdatePublicationHandler(
                publicationService, ticketService, environment, identityServiceClient,
                getUriRetriever(getHttpClientWithUnresolvableClient(), secretsManagerClient));
        updatePublicationHandler.handleRequest(inputStream, output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        var problem = response.getBodyObject(Problem.class);

        assertThat(response.getStatusCode(), Is.is(IsEqual.equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(problem.getDetail(), Is.is(
                IsEqual.equalTo(UNABLE_TO_FETCH_CUSTOMER_ERROR_MESSAGE)));
    }

    private void injectContributor(Publication savedPublication, Contributor contributor) {
        var contributors = savedPublication.getEntityDescription().getContributors();
        contributors.add(contributor);
        savedPublication.getEntityDescription().setContributors(contributors);
        publicationService.updatePublication(savedPublication);
    }

    private Contributor createContributorForPublicationUpdate(URI cristinId) {
        return new Contributor.Builder()
                .withRole(new RoleType(Role.ARCHITECT))
                .withIdentity(new Identity.Builder().withId(cristinId).withName(randomString()).build())
                .build();
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

    private InputStream userUpdatesPublicationOfOtherInstitution(Publication publicationUpdate)
            throws JsonProcessingException {
        var pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationUpdate.getIdentifier().toString());
        URI customerId = randomUri();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                .withPathParameters(pathParameters)
                .withCurrentCustomer(customerId)
                .withBody(publicationUpdate)
                .withAccessRights(customerId, EDIT_OWN_INSTITUTION_RESOURCES.toString())
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
                .withAccessRights(customerId, EDIT_OWN_INSTITUTION_RESOURCES.name(), PUBLISH_THESIS.name())
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
                .withAccessRights(customerId, EDIT_OWN_INSTITUTION_RESOURCES.name(), PUBLISH_THESIS.name())
                .build();
    }

    private InputStream ownerUpdatesOwnPublication(SortableIdentifier publicationIdentifier,
                                                   Publication publicationUpdate)
            throws JsonProcessingException {
        Map<String, String> pathParameters = Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString());

        var customerId = publicationUpdate.getPublisher().getId();
        return new HandlerRequestBuilder<Publication>(restApiMapper)
                .withUserName(publicationUpdate.getResourceOwner().getOwner().getValue())
                .withCurrentCustomer(customerId)
                .withBody(publicationUpdate)
                .withAccessRights(customerId, EDIT_OWN_INSTITUTION_RESOURCES.name(), PUBLISH_THESIS.name())
                .withPathParameters(pathParameters)
                .build();
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
                false, false, null);
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

    private AuthorizedBackendUriRetriever getUriRetriever(FakeHttpClient<String> httpClient,
                                                          SecretsManagerClient secretsManagerClient) {
        return new AuthorizedBackendUriRetriever(httpClient,
                secretsManagerClient,
                BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME);
    }
}
