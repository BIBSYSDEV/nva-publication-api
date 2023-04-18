package no.unit.nva.publication.ticket.create;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.business.TicketEntry.SUPPORT_SERVICE_CORRESPONDENT;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.LOCATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.GeneralSupportRequestDto;
import no.unit.nva.publication.ticket.PublishingRequestDto;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.Problem;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@WireMockTest(httpsEnabled = true)
class CreateTicketHandlerTest extends TicketTestLocal {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String ACCESS_TOKEN_RESPONSE_BODY = "{ \"access_token\" : \"Bearer token\"}";
    private final Environment environment = mock(Environment.class);
    private FakeSecretsManagerClient secretsManagerClient;
    private CreateTicketHandler handler;
    private TicketResolver ticketResolver;

    public static Stream<Arguments> ticketEntryProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class).map(Arguments::of);
    }

    @BeforeEach
    public void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        super.init();
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.readEnv("API_HOST")).thenReturn(wireMockRuntimeInfo.getHttpsBaseUrl());
        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("someSecret", credentials.toString());
        var uriRetriever = getUriRetriever(getHttpClientWithPublisherAllowingPublishing(), secretsManagerClient);
        ticketResolver = new TicketResolver(resourceService, ticketService, uriRetriever);
        this.handler = new CreateTicketHandler(resourceService, ticketResolver);
    }

    @ParameterizedTest
    @DisplayName("should persist ticket when publication exists, user is publication owner and "
                 + "publication meets ticket creation criteria")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldPersistTicketWhenPublicationExistsUserIsOwnerAndPublicationMeetsTicketCreationCriteria(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws IOException, ApiGatewayException {

        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var owner = UserInstance.fromPublication(publication);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));

        var location = URI.create(response.getHeaders().get(LOCATION_HEADER));

        assertThatLocationHeaderHasExpectedFormat(publication, location);
        assertThatLocationHeaderPointsToCreatedTicket(location);
    }

    @ParameterizedTest(name = "should be possible to create DoiTicket for published publication")
    @EnumSource(value = PublicationStatus.class, names = {"PUBLISHED", "PUBLISHED_METADATA"})
    void shouldBePossibleToCreateDoiTicketForPublishedPublication(PublicationStatus publicationStatus)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(publicationStatus,
                                                                     resourceService);
        var requestBody = constructDto(DoiRequest.class);
        var owner = UserInstance.fromPublication(publication);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        var location = URI.create(response.getHeaders().get(LOCATION_HEADER));
        assertThatLocationHeaderHasExpectedFormat(publication, location);
        assertThatLocationHeaderPointsToCreatedTicket(location);
    }

    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should should not allow creating a ticket for non existing publication")
    @MethodSource("ticketEntryProvider")
    void shouldNotAllowTicketCreationForNonExistingPublication(Class<? extends TicketEntry> ticketType)
        throws IOException {
        var publication = nonPersistedPublication();
        var requestBody = constructDto(ticketType);
        var owner = UserInstance.fromPublication(publication);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @DisplayName("should not allow users to create tickets for publications they do not own")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotAllowUsersToCreateTicketsForPublicationsTheyDoNotOwn(Class<? extends TicketEntry> ticketType,
                                                                       PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var input = createHttpTicketCreationRequest(requestBody, publication, user);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @DisplayName("should not allow users to create tickets for publications belonging to different organization"
                 + "than the one they are currently logged in to")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotAllowUsersToCreateTicketsForPublicationsBelongingToDifferentOrgThanTheOneTheyAreLoggedInTo(
        Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(publication.getResourceOwner().getOwner(), randomUri());
        var input = createHttpTicketCreationRequest(requestBody, publication, user);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @DisplayName("should not allow anonymous users to create tickets")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotAllowAnonymousUsersToCreateTickets(Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var input = createAnonymousHttpTicketCreationRequest(requestBody, publication);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }

    @Test
    void shouldNotAllowPublishingRequestTicketCreationWhenPublicationIsNotPublishable()
        throws IOException, BadRequestException {
        var publication = createUnpublishablePublication();
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(PublishingRequestCase.class);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as read for the publication owner when publication owner creates new ticket")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsReadForThePublicationOwnerWhenPublicationOwnerCreatesNewTicket(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(ticketType);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        TicketEntry ticket = fetchTicket(response);
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
    }

    @ParameterizedTest
    @DisplayName("should mark ticket as Unread for the Curators when publication owner creates new ticket")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsUnReadForTheCuratorsWhenPublicationOwnerCreatesNewTicket(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(ticketType);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        TicketEntry ticket = fetchTicket(response);
        assertThat(ticket.getViewedBy(), not(hasItem(SUPPORT_SERVICE_CORRESPONDENT)));
    }

    @DisplayName("should update existing DoiRequest when new DOI is requested but a DoiRequest that has not been "
                 + "fulfilled already exists")
    @Test
    void shouldUpdateExistingDoiRequestWhenNewDoiIsRequestedButUnfulfilledDoiRequestAlreadyExists()
        throws ApiGatewayException, IOException {
        var publication = createPersistedPublishedPublication();
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(DoiRequest.class);
        ticketResolver = new TicketResolver(resourceService, ticketService, getUriRetriever(getHttpClientWithUnresolvableClient(),
                                                                                                                  secretsManagerClient));
        this.handler = new CreateTicketHandler(resourceService, ticketResolver);
        var firstRequest = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(firstRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));

        var createdTicket = fetchTicket(response).copy();
        var secondRequest = createHttpTicketCreationRequest(requestBody, publication, owner);
        output = new ByteArrayOutputStream();
        ticketResolver = new TicketResolver(resourceService, ticketService, getUriRetriever(getHttpClientWithUnresolvableClient(),
                                                                                                                  secretsManagerClient));
        this.handler = new CreateTicketHandler(resourceService, ticketResolver);
        handler.handleRequest(secondRequest, output, CONTEXT);

        var secondResponse = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(secondResponse.getStatusCode(), is(equalTo(HTTP_CREATED)));

        var existingTicket = fetchTicket(secondResponse);
        assertThat(existingTicket.getModifiedDate(), is(greaterThan(createdTicket.getModifiedDate())));
    }

    @Test
    void shouldLogErrorWhenErrorOccurs() throws IOException {
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        var requestBody = constructDto(DoiRequest.class);
        var ownerForOnePublication = UserInstance.fromPublication(randomPublication());
        var anotherPublication = randomPublication();
        var input = createHttpTicketCreationRequest(requestBody, anotherPublication, ownerForOnePublication);
        handler.handleRequest(input, output, CONTEXT);
        assertThat(appender.getMessages(), containsString("Request failed:"));
    }

    @Test
    void shouldAllowUserWithDoiRequestApprovalAccessRight() throws ApiGatewayException, IOException {
        var publication = createPersistedPublishedPublication();
        var publicationOwner = publication.getPublisher().getId();
        var requestBody = constructDto(DoiRequest.class);
        var request = createHttpTicketCreationRequestWithApprovedAccessRight(requestBody, publication, publicationOwner,
                                                                             AccessRight.APPROVE_DOI_REQUEST);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @Test
    void shouldAllowUserWithDoiRequestRejectionAccessRight() throws ApiGatewayException, IOException {
        var publication = createPersistedPublishedPublication();
        var publicationOwner = publication.getPublisher().getId();
        var requestBody = constructDto(DoiRequest.class);
        var request = createHttpTicketCreationRequestWithApprovedAccessRight(requestBody, publication, publicationOwner,
                                                                             AccessRight.REJECT_DOI_REQUEST);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @Test
    void shouldNotAllowUserWithDoiRequestToCreateTicketForPublicationFromAnotherInstitution()
        throws ApiGatewayException, IOException {
        var publication = createPersistedPublishedPublication();
        var customerId = randomUri();
        assertThat(customerId, is(not(equalTo(publication.getPublisher().getId()))));
        var requestBody = constructDto(DoiRequest.class);
        var request = createHttpTicketCreationRequestWithApprovedAccessRight(requestBody, publication, customerId,
                                                                             AccessRight.REJECT_DOI_REQUEST);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldPublishPublicationAndSetTicketStatusToApprovedWhenCustomerAllowsPublishing()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService, getUriRetriever(getHttpClientWithPublisherAllowingPublishing(),
                                                                                                                  secretsManagerClient));
        handler = new CreateTicketHandler(resourceService, ticketResolver);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(resourceService.getPublication(publication).getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(TicketStatus.COMPLETED)));
    }

    @Test
    void shouldSetTicketWorkflowWhenCustomerAllowsPublishingMetadataOnly()
            throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService, getUriRetriever(getHttpClientWithCustomerAllowingPublishingMetadataOnly(),
                secretsManagerClient));
        handler = new CreateTicketHandler(resourceService, ticketResolver);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(TicketStatus.PENDING)));
        assertThat(getTicketPublishingWorkflow(publication), is(equalTo(PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)));
    }

    @Test
    void shouldSetTicketWorkflowWhenCustomerRequiresApprovalForMetadataAndFiles()
            throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService, getUriRetriever(getHttpClientWithPublisherRequiringApproval(),
                secretsManagerClient));
        handler = new CreateTicketHandler(resourceService, ticketResolver);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(TicketStatus.PENDING)));
        assertThat(getTicketPublishingWorkflow(publication), is(equalTo(PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES)));
    }

    @Test
    void shouldPublishPublicationAndFileAndSetTicketStatusToApprovedWhenCustomerAllowsPublishing()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithUnpublishedFiles(PublicationStatus.DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService, getUriRetriever(getHttpClientWithPublisherAllowingPublishing(),
                                                                                                                  secretsManagerClient));
        handler = new CreateTicketHandler(resourceService, ticketResolver);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        var publishedPublication = resourceService.getPublication(publication);
        assertThat(getAssociatedFiles(publishedPublication), everyItem(instanceOf(PublishedFile.class)));
        assertThat(publishedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(TicketStatus.COMPLETED)));
    }

    @Test
    void shouldReturnBadGatewayWhenHttpClientWithNonResolvablePublishingWorkflow() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService, getUriRetriever(getHttpClientWithNonResolvedPublishingWorkflow(),
                secretsManagerClient));
        this.handler = new CreateTicketHandler(resourceService, ticketResolver);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var problem = response.getBodyObject(Problem.class);

        assertThat(problem.getDetail(), is(equalTo("Unable to resolve customer publishing workflow")));
        assertThat(resourceService.getPublication(publication).getStatus(), is(equalTo(PublicationStatus.DRAFT)));
    }

    @Test
    void shouldReturnBadGatewayWhenHttpClientUnableToRetrievePublishingWorkflow() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService, getUriRetriever(getHttpClientWithUnresolvableClient(),
                secretsManagerClient));
        this.handler = new CreateTicketHandler(resourceService, ticketResolver);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var problem = response.getBodyObject(Problem.class);

        assertThat(problem.getDetail(), is(equalTo("Unable to fetch customer publishing workflow from upstream")));
        assertThat(resourceService.getPublication(publication).getStatus(), is(equalTo(PublicationStatus.DRAFT)));
    }

    private static List<AssociatedArtifact> getAssociatedFiles(Publication publishedPublication) {
        return publishedPublication.getAssociatedArtifacts()
                   .stream()
                   .filter(artifact -> artifact instanceof File)
                   .collect(Collectors.toList());

    }

    private static FakeHttpClient<String> getHttpClientWithUnresolvableClient() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY,
                                                            HTTP_OK), unresolvableCustomer());
    }

    private static FakeHttpClient<String> getHttpClientWithPublisherAllowingPublishing() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK),
                                    mockIdentityServiceResponseForPublisherAllowingAutomaticPublishingRequestsApproval());
    }

    private static FakeHttpClient<String> getHttpClientWithCustomerAllowingPublishingMetadataOnly() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK),
                mockIdentityServiceResponseCustomerAllowingPublishingMetadataOnly());
    }

    private static FakeHttpClient<String> getHttpClientWithPublisherRequiringApproval() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK),
                mockIdentityServiceResponseForCustomerRequiringApprovalForPublishing());
    }

    private static FakeHttpClient<String> getHttpClientWithNonResolvedPublishingWorkflow() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK),
                                    mockIdentityServiceResponseForNonResolvedPublishingWorkflow());
    }

    private static FakeHttpResponse<String> mockIdentityServiceResponseForNonResolvedPublishingWorkflow() {
        return FakeHttpResponse.create(IoUtils.stringFromResources(Path.of("unrecognizable_publishing_workflow.json")),
                                       HTTP_OK);
    }

    private static FakeHttpResponse<String> mockIdentityServiceResponseForPublisherAllowingAutomaticPublishingRequestsApproval() {
        return FakeHttpResponse.create(IoUtils.stringFromResources(Path.of("customer_allowing_publishing.json")),
                                       HTTP_OK);
    }

    private static FakeHttpResponse<String> mockIdentityServiceResponseCustomerAllowingPublishingMetadataOnly() {
        return FakeHttpResponse.create(IoUtils.stringFromResources(Path.of("customer_allowing_publishing_metadata_only.json")),
                HTTP_OK);
    }

    private static FakeHttpResponse<String> mockIdentityServiceResponseForCustomerRequiringApprovalForPublishing() {
        return FakeHttpResponse.create(IoUtils.stringFromResources(Path.of("customer_requires_approval_for_publishing.json")),
                HTTP_OK);
    }

    private PublishingWorkflow getTicketPublishingWorkflow(Publication publication) {
        return getPublishingRequestCase(publication).getWorkflow();
    }

    private PublishingRequestCase getPublishingRequestCase(Publication publication) {
        return ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                publication.getIdentifier(),
                PublishingRequestCase.class).get();
    }

    private static SortableIdentifier extractTicketIdentifierFromLocation(URI location) {
        return new SortableIdentifier(UriWrapper.fromUri(location).getLastPathElement());
    }

    private static void assertThatLocationHeaderHasExpectedFormat(Publication publication, URI location) {
        var ticketIdentifier = extractTicketIdentifierFromLocation(location);
        var publicationIdentifier = extractPublicationIdentifierFromLocation(location);
        assertThat(publicationIdentifier, is(equalTo(publication.getIdentifier())));
        assertThat(ticketIdentifier, is(not(nullValue())));
    }

    private static SortableIdentifier extractPublicationIdentifierFromLocation(URI location) {
        return UriWrapper.fromUri(location)
                   .getParent()
                   .flatMap(UriWrapper::getParent)
                   .map(UriWrapper::getLastPathElement)
                   .map(SortableIdentifier::new)
                   .orElseThrow();
    }

    private static FakeHttpResponse<String> unresolvableCustomer() {
        return FakeHttpResponse.create(randomString(), HTTP_NOT_FOUND);
    }

    private AuthorizedBackendUriRetriever getUriRetriever(FakeHttpClient<String> httpClient,
                                                          SecretsManagerClient secretsManagerClient) {
        return new AuthorizedBackendUriRetriever(httpClient,
                                                 secretsManagerClient,
                                                 BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME);
    }

    private TicketStatus getTicketStatusForPublication(Publication publication) {
        return getPublishingRequestCase(publication).getStatus();
    }

    private TicketEntry fetchTicket(GatewayResponse<Void> response) throws NotFoundException {
        var ticketIdentifier = new SortableIdentifier(UriWrapper.fromUri(response.getHeaders().get(LOCATION_HEADER))
                                                          .getLastPathElement());
        return ticketService.fetchTicketByIdentifier(ticketIdentifier);
    }

    private Publication createUnpublishablePublication() throws BadRequestException {
        var publication = randomPublication().copy().withEntityDescription(null).build();
        publication = Resource.fromPublication(publication)
                          .persistNew(resourceService, UserInstance.fromPublication(publication));
        return publication;
    }

    private void assertThatLocationHeaderPointsToCreatedTicket(URI ticketUri)
        throws NotFoundException {
        var publication = fetchPublication(ticketUri);
        var ticketIdentifier = extractTicketIdentifierFromLocation(ticketUri);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);
        assertThat(ticket.extractPublicationIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(ticket.getIdentifier(), is(equalTo(ticketIdentifier)));
    }

    private Publication fetchPublication(URI ticketUri) throws NotFoundException {
        var publicationIdentifier = extractPublicationIdentifierFromLocation(ticketUri);
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private TicketDto constructDto(Class<? extends TicketEntry> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            return DoiRequestDto.empty();
        } else if (PublishingRequestCase.class.equals(ticketType)) {
            return PublishingRequestDto.empty();
        } else if (GeneralSupportRequest.class.equals(ticketType)) {
            return GeneralSupportRequestDto.empty();
        }

        throw new RuntimeException("Unrecognized ticket type");
    }

    private Publication createPersistedPublishedPublication() throws ApiGatewayException {
        var publication = randomPublication();
        publication.setDoi(null); // for creating DoiRequests
        publication = Resource.fromPublication(publication)
                          .persistNew(resourceService, UserInstance.fromPublication(publication));
        resourceService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        return resourceService.getPublication(publication);
    }

    private InputStream createHttpTicketCreationRequest(TicketDto ticketDto,
                                                        Publication publication,
                                                        UserInstance userCredentials)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withUserName(userCredentials.getUsername())
                   .withCurrentCustomer(userCredentials.getOrganizationUri())
                   .build();
    }

    private InputStream createHttpTicketCreationRequestWithApprovedAccessRight(TicketDto ticketDto,
                                                                               Publication publication,
                                                                               URI customerId,
                                                                               AccessRight accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withAccessRights(customerId, accessRight.toString())
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withUserName(randomString())
                   .withCurrentCustomer(customerId)
                   .build();
    }

    private InputStream createAnonymousHttpTicketCreationRequest(TicketDto ticketDto,
                                                                 Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .build();
    }
}
