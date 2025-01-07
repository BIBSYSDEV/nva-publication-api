package no.unit.nva.publication.ticket.create;

import static com.github.tomakehurst.wiremock.common.ContentTypes.APPLICATION_JSON;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomContributorWithIdAndAffiliation;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_AUTH_URL;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.BACKEND_CLIENT_SECRET_NAME;
import static no.unit.nva.publication.ticket.create.CreateTicketHandler.LOCATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.GeneralSupportRequestDto;
import no.unit.nva.publication.ticket.MessageDto;
import no.unit.nva.publication.ticket.PublishingRequestDto;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketTestLocal;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.JwtTestToken;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
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

class CreateTicketHandlerTest extends TicketTestLocal {

    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    public static final String ACCESS_TOKEN_RESPONSE_BODY = """
        { "access_token" : "%s"}
        """.formatted(JwtTestToken.randomToken());
    private static final String PERSON_AFFILIATION_CLAIM = "custom:personAffiliation";

    private FakeSecretsManagerClient secretsManagerClient;
    private CreateTicketHandler handler;
    private TicketResolver ticketResolver;
    private MessageService messageService;

    public static Stream<Arguments> ticketEntryProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class)
                   .filter(type -> !type.getPayload().equals(UnpublishRequest.class))
                   .map(Arguments::of);
    }

    @BeforeEach
    public void setup() {
        super.init();
        secretsManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretsManagerClient.putPlainTextSecret("someSecret", credentials.toString());
        var uriRetriever = getUriRetriever(getHttpClientWithPublisherAllowingPublishing(), secretsManagerClient);
        ticketResolver = new TicketResolver(resourceService, ticketService, uriRetriever);
        messageService = new MessageService(client, new UriRetriever());
        this.handler = new CreateTicketHandler(ticketResolver, messageService);
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
        var user = UserInstance.create(randomString(), publication.getPublisher().getId(),
                                       randomUri(), List.of(), publication.getResourceOwner().getOwnerAffiliation());
        var input = createHttpTicketCreationRequest(requestBody, publication, user);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @DisplayName("should not allow users to create tickets for publications they do not belong to, i.e. "
                 + "where they are not listed as contributors, owner or curators for owner or contributors institution")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldNotAllowUsersToCreateTicketsForPublicationsBelongingToDifferentOrgThanTheOneTheyAreLoggedInTo(
        Class<? extends TicketEntry> ticketType, PublicationStatus status)
        throws IOException, ApiGatewayException {
        var publication = TicketTestUtils.createPersistedPublication(status, resourceService);
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(randomString(), randomUri(), randomUri(), List.of(), randomUri());
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

    @ParameterizedTest
    @DisplayName("should mark ticket as read for only the publication owner when publication owner creates new "
                 + "ticket")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldMarkTicketAsReadForThePublicationOwnerWhenPublicationOwnerCreatesNewTicket(
        Class<? extends TicketEntry> ticketType, PublicationStatus status) throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), status, resourceService);
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(ticketType);
        var input = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(input, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        TicketEntry ticket = fetchTicket(response);
        assertThat(ticket.getViewedBy().size(), is(equalTo(1)));
        assertThat(ticket.getViewedBy(), hasItem(ticket.getOwner()));
    }

    @DisplayName("should update existing DoiRequest when new DOI is requested but a DoiRequest that has not been "
                 + "fulfilled already exists")
    @Test
    void shouldUpdateExistingDoiRequestWhenNewDoiIsRequestedButUnfulfilledDoiRequestAlreadyExists()
        throws ApiGatewayException, IOException {
        var publication = createPersistedNonDegreePublishedPublication();
        var owner = UserInstance.fromPublication(publication);
        var requestBody = constructDto(DoiRequest.class);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithUnresolvableClient(),
                                                            secretsManagerClient));
        this.handler = new CreateTicketHandler(ticketResolver, messageService);
        var firstRequest = createHttpTicketCreationRequest(requestBody, publication, owner);
        handler.handleRequest(firstRequest, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));

        final var createdTicket = fetchTicket(response).copy();
        final var secondRequest = createHttpTicketCreationRequest(requestBody, publication, owner);
        output = new ByteArrayOutputStream();
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithUnresolvableClient(),
                                                            secretsManagerClient));
        this.handler = new CreateTicketHandler(ticketResolver, messageService);
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
    void shouldNotAllowUserWithManageDoiAccessRightToCreateDoiRequestForPublicationUserDoesNotBelongsTo()
        throws ApiGatewayException, IOException {
        var publication = createPersistedPublishedPublication();
        var requestBody = constructDto(DoiRequest.class);
        var request = createHttpTicketCreationRequestWithApprovedAccessRight(requestBody, publication, randomUri(),
                                                                             AccessRight.MANAGE_DOI);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void shouldSetTicketStatusToApprovedWhenCustomerAllowsPublishing()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithPublisherAllowingPublishing(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(COMPLETED)));
    }

    @Test
    void shouldSetTicketWorkflowWhenCustomerAllowsPublishingMetadataOnly()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithCustomerAllowingPublishingMetadataOnly(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(TicketStatus.PENDING)));
        assertThat(getTicketPublishingWorkflow(publication),
                   is(equalTo(PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)));
    }

    @Test
    void shouldSetTicketWorkflowWhenCustomerRequiresApprovalForMetadataAndFiles()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithPublisherRequiringApproval(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(TicketStatus.PENDING)));
        assertThat(getTicketPublishingWorkflow(publication),
                   is(equalTo(PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES)));
    }

    @Test
    void shouldCreatePendingPublishingRequestWhenCustomerAllowsPublishingMetadataOnly()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithCustomerAllowingPublishingMetadataOnly(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        var publishedPublication = resourceService.getPublication(publication);

        assertThat(getAssociatedFiles(publishedPublication), everyItem(instanceOf(PendingOpenFile.class)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(TicketStatus.PENDING)));
    }

    @Test
    void shouldPersistCompletedPublishingRequestWhenPublicationIsWithoutFilesAndCustomerAllowsPublishingMetadataOnly()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithAssociatedLink(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithCustomerAllowingPublishingMetadataOnly(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(getTicketStatusForPublication(publication), is(equalTo(TicketStatus.COMPLETED)));
    }

    @Test
    void shouldReturnInternalErrorWhenHttpClientWithNonResolvablePublishingWorkflow()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithNonResolvedPublishingWorkflow(),
                                                            secretsManagerClient));
        this.handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
        assertThat(resourceService.getPublication(publication).getStatus(), is(equalTo(DRAFT)));
    }

    @Test
    void shouldReturnBadGatewayWhenHttpClientUnableToRetrievePublishingWorkflow()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithUnresolvableClient(),
                                                            secretsManagerClient));
        this.handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));

        var problem = response.getBodyObject(Problem.class);

        assertThat(problem.getDetail(), is(equalTo("Unable to fetch customerId publishing workflow from upstream")));
        assertThat(resourceService.getPublication(publication).getStatus(), is(equalTo(DRAFT)));
    }

    @Test
    void shouldSetApprovedFilesForPublishingRequestWhenUserCanPublishFiles()
        throws ApiGatewayException, IOException {
        var publication =
            TicketTestUtils.createPersistedPublicationWithPendingOpenFile(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithPublisherAllowingPublishing(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));

        var publishingRequest = ticketService.fetchTicketByResourceIdentifier(
            publication.getPublisher().getId(), publication.getIdentifier(), PublishingRequestCase.class);

        var expectedApprovedFiles = publication.getAssociatedArtifacts().stream()
                                        .filter(PendingFile.class::isInstance)
                                        .map(File.class::cast)
                                        .map(File::getIdentifier)
                                        .toArray(UUID[]::new);

        var approvedFilesIdentifiers = publishingRequest.orElseThrow().getApprovedFiles().stream()
                                           .map(File::getIdentifier)
                                           .collect(Collectors.toSet());
        assertThat(approvedFilesIdentifiers, containsInAnyOrder(expectedApprovedFiles));
    }

    @Test
    void shouldEmptyFilesForApprovalWhenUserCanPublishFiles() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithPublisherAllowingPublishing(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));

        var publishingRequest = ticketService.fetchTicketByResourceIdentifier(
            publication.getPublisher().getId(), publication.getIdentifier(), PublishingRequestCase.class).orElseThrow();

        assertThat(publishingRequest.getFilesForApproval(), is(emptyIterable()));
    }

    static Stream<Arguments> fileTypesNeedingApprovalProvider() {
        return Stream.of(Arguments.of(randomPendingOpenFile()),
                         Arguments.of(randomPendingInternalFile()));
    }

    @ParameterizedTest
    @MethodSource("fileTypesNeedingApprovalProvider")
    void shouldCreatePublishingRequestWithFilesForApprovalWhenPublicationHasFilesThatNeedApproval(File file)
        throws ApiGatewayException, IOException
    {
        var publication = TicketTestUtils.createPersistedPublicationWithFile(DRAFT, file, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(publication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithPublisherRequiringApproval(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, publication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));

        var publishingRequest = ticketService.fetchTicketByResourceIdentifier(
            publication.getPublisher().getId(), publication.getIdentifier(), PublishingRequestCase.class).orElseThrow();

        assertThat(publishingRequest.getFilesForApproval(), containsInAnyOrder(file));
    }

    @Test
    void userWithAccessRightManageDoiShouldNotBeAbleToAutoPublishFilesWhenAllowedToPublishMetadataOnly()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithPendingOpenFile(DRAFT, resourceService);
        var requestBody = constructDto(PublishingRequestCase.class);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithCustomerAllowingPublishingMetadataOnly(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(
            createHttpTicketCreationRequestWithAccessRight(
                requestBody, publication, AccessRight.MANAGE_DOI), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        var updatedPublication = resourceService.getPublication(publication);
        var file = (File) updatedPublication.getAssociatedArtifacts().getFirst();

        assertThat(file, is(instanceOf(PendingOpenFile.class)));
    }

    @Test
    void shouldSetFinalizedByFromRequestUtilsWhenTicketIsAutoApproved()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED, resourceService);
        var curatorName = randomString();
        var requestBody = constructDto(PublishingRequestCase.class);
        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(), curatingInstitution,
                                                      randomUri(), curatorName, MANAGE_PUBLISHING_REQUESTS);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));

        var ticket = fetchTicket(response);

        assertThat(ticket.getFinalizedBy().toString(), is(not(equalTo(publication.getResourceOwner().getOwner()))));
        assertThat(ticket.getFinalizedBy().toString(), is(equalTo(curatorName)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#invalidAccessRightForTicketTypeProvider")
    void shouldNotAllowCuratorWithoutValidAccessRightToCreateTicket(Class<? extends TicketEntry> ticketType,
                                                                    AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), DRAFT, resourceService);
        var requestBody = constructDto(ticketType);
        var user = UserInstance.create(randomString(), publication.getPublisher().getId());
        var input = createHttpTicketCreationRequestWithApprovedAccessRight(
            requestBody, publication, user.getUsername(), user.getCustomerId(), accessRights);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldAllowCuratorWithValidAccessRightToCreateTicket(PublicationStatus status,
                                                              Class<? extends TicketEntry> ticketType,
                                                                    AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), status, resourceService);
        var requestBody = constructDto(ticketType);
        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var input = createHttpTicketCreationRequest(
            requestBody, publication.getIdentifier(), curatingInstitution, randomUri(), randomString(), accessRights);
        handler.handleRequest(input, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("ticketEntryProvider")
    void shouldAllowContributorToCreateTicketForNonDegreePublication(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED, resourceService);
        var requestBody = constructDto(ticketType);

        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var contributorCristinId = publication.getEntityDescription().getContributors().getFirst().getIdentity().getId();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, contributorCristinId, randomString());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldAllowCuratorWithValidAccessRightAndRelatedToContributorToCreateTicketForNonDegreePublication(
        PublicationStatus publicationStatus, Class<? extends TicketEntry> ticketType, AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), publicationStatus, resourceService);
        var requestBody = constructDto(ticketType);

        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, randomUri(), randomString(),accessRights);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("ticketEntryProvider")
    void shouldNotAllowContributorToCreateTicketForDegreePublication(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PUBLISHED, resourceService);
        var requestBody = constructDto(ticketType);

        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var contributorCristinId = publication.getEntityDescription().getContributors().getFirst().getIdentity().getId();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, contributorCristinId, randomString());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldNotAllowCuratorWithValidAccessRightAndRelatedContributorToCreateTicketForDegreePublication(
        PublicationStatus publicationStatus, Class<? extends TicketEntry> ticketType, AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(publicationStatus, resourceService);
        var requestBody = constructDto(ticketType);

        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, randomUri(), randomString(),accessRights);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @ParameterizedTest
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndAccessRightProvider")
    void shouldNotAllowNotRelatedCuratorWithValidAccessRightForNonDegreePublication(
        PublicationStatus publicationStatus, Class<? extends TicketEntry> ticketType, AccessRight... accessRights)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), publicationStatus,
                                                                              resourceService);
        var requestBody = constructDto(ticketType);

        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      randomUri(), randomUri(), randomString(),accessRights);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_FORBIDDEN)));
    }

    @Test
    void contributorAtAnotherInstitutionThanPublicationOwnerShouldBeAbleToCreateTicketForPublication()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var contributorId = randomUri();
        var affiliationId = randomUri();
        publication.getEntityDescription().setContributors(List.of(randomContributorWithIdAndAffiliation(contributorId, affiliationId)));
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(affiliationId, Set.of(contributorId))));
        mockCuratingInstitution(affiliationId);
        resourceService.updatePublication(publication);
        var requestBody = constructDto(DoiRequest.class);

        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      affiliationId, contributorId, randomString());
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    private void mockCuratingInstitution(URI affiliationId) {
        when(uriRetriever.getRawContent(affiliationId, APPLICATION_JSON))
            .thenReturn(Optional.of("""
                                        {
                                          "type": "Organization",
                                          "partOf": [
                                            {
                                              "type": "Organization",
                                              "id": "%s"
                                            }
                                          ]
                                        }
                                        """.formatted(affiliationId)));
    }

    @Test
    void curatorAtAnotherInstitutionThanPublicationOwnerShouldBeAbleToCreateTicketForPublication()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PUBLISHED, resourceService);
        var curatingInstitution = randomUri();
        publication.setCuratingInstitutions(Set.of(new CuratingInstitution(curatingInstitution, Set.of(randomUri()))));
        resourceService.updatePublication(publication);
        var requestBody = constructDto(PublishingRequestCase.class);

        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, randomUri(), randomString(),
                                                      MANAGE_PUBLISHING_REQUESTS);
        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @Test
    void shouldPersistMessageWhenCreatingTicketWithMessage()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED,
                                                                              resourceService);
        var message = Message.builder().withText(randomString()).build();
        var requestBody = GeneralSupportRequestDto.builder()
                         .withMessages(List.of(MessageDto.fromMessage(message)))
                         .build(new GeneralSupportRequest());


        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      publication.getResourceOwner().getOwnerAffiliation(),
                                                      randomUri(), randomString(),
                                                      MANAGE_RESOURCES_STANDARD, SUPPORT);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var ticket = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                         .toList().getFirst();
        var persistedMessage = ticket.fetchMessages(ticketService).getFirst();

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(persistedMessage.getText(), is(equalTo(message.getText())));
    }

    @Test
    void shouldPersistSingleMessageWhenCreatingTicketWithMultipleMessage()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED,
                                                                              resourceService);
        var firstMessage = Message.builder().withText(randomString()).build();
        var secondMessage = Message.builder().withText(randomString()).build();
        var requestBody = GeneralSupportRequestDto.builder()
                              .withMessages(List.of(MessageDto.fromMessage(firstMessage),
                                                    MessageDto.fromMessage(secondMessage)))
                              .build(new GeneralSupportRequest());


        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      publication.getResourceOwner().getOwnerAffiliation(),
                                                      randomUri(), randomString(),
                                                      MANAGE_RESOURCES_STANDARD, SUPPORT);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var ticket = resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication))
                         .toList().getFirst();
        var persistedMessage = ticket.fetchMessages(ticketService);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
        assertThat(persistedMessage, hasSize(1));
    }

    @Test
    void creatingTicketWithoutMessageShouldNotLogAnyMessages()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED,
                                                                              resourceService);

        var request = createHttpTicketCreationRequest(
            constructDto(GeneralSupportRequest.class), publication.getIdentifier(),
            publication.getResourceOwner().getOwnerAffiliation(), randomUri(), randomString(),
            MANAGE_RESOURCES_STANDARD, SUPPORT);
        var logAppender = LogUtils.getTestingAppender(CreateTicketHandler.class);
        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);
        var logMessages = logAppender.getMessages();

        assertThat(logMessages, is(emptyString()));
        assertThat(response.getStatusCode(), is(equalTo(HTTP_CREATED)));
    }

    @ParameterizedTest
    @MethodSource("ticketEntryProvider")
    void shouldSetUserFromRequestAsTicketOwner(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedNonDegreePublication(randomUri(), PUBLISHED, resourceService);
        var requestBody = constructDto(ticketType);
        var curatingInstitution = publication.getCuratingInstitutions().iterator().next().id();

        var userId = publication.getEntityDescription().getContributors().getFirst().getIdentity().getId();
        var username = randomString();
        var request = createHttpTicketCreationRequest(requestBody, publication.getIdentifier(),
                                                      curatingInstitution, userId, username);
        handler.handleRequest(request, output, CONTEXT);

        var ticket =
            resourceService.fetchAllTicketsForResource(Resource.fromPublication(publication)).toList().getFirst();

        assertThat(ticket.getOwner().toString(), is(equalTo(username)));
    }

    @Test
    void shouldReturnConflictWhenCreatingCompletedPublishingRequestWithInvalidFilesWhenCustomerAllowsPublishingFiles()
        throws ApiGatewayException, IOException {
        var fileWithoutLicense = randomPendingOpenFile().copy().withLicense(null).buildPendingOpenFile();
        var publication = randomPublication().copy().withAssociatedArtifacts(List.of(fileWithoutLicense)).build();
        var persistedPublication = resourceService.createPublication(UserInstance.fromPublication(publication),
                                                                 publication);
        var requestBody = constructDto(PublishingRequestCase.class);
        var owner = UserInstance.fromPublication(persistedPublication);
        ticketResolver = new TicketResolver(resourceService, ticketService,
                                            getUriRetriever(getHttpClientWithPublisherAllowingPublishing(),
                                                            secretsManagerClient));
        handler = new CreateTicketHandler(ticketResolver, messageService);
        handler.handleRequest(createHttpTicketCreationRequest(requestBody, persistedPublication, owner), output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertThat(response.getStatusCode(), is(equalTo(HTTP_CONFLICT)));
        assertThat(response.getBodyObject(Problem.class).getDetail(),
                   is(equalTo("Cannot publish a file without a license: " + fileWithoutLicense.getIdentifier())));
    }


    private TicketEntry fetchTicket(GatewayResponse<Void> response) throws NotFoundException {
        var ticketIdentifier = new SortableIdentifier(UriWrapper.fromUri(response.getHeaders().get(LOCATION_HEADER))
                                                          .getLastPathElement());
        return ticketService.fetchTicketByIdentifier(ticketIdentifier);
    }

    private static List<AssociatedArtifact> getAssociatedFiles(Publication publishedPublication) {
        return publishedPublication.getAssociatedArtifacts()
                   .stream()
                   .filter(artifact -> artifact instanceof File)
                   .toList();
    }

    private static FakeHttpClient<String> getHttpClientWithUnresolvableClient() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY,
                                                            HTTP_OK), unresolvableCustomer());
    }

    private static FakeHttpClient<String> getHttpClientWithPublisherAllowingPublishing() {
        return new FakeHttpClient<>(FakeHttpResponse.create(ACCESS_TOKEN_RESPONSE_BODY, HTTP_OK),
                                    mockIdentityServiceResponseForPublisherAllowingAutomaticPublishing());
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
        var path = Path.of("unrecognizable_publishing_workflow.json");
        return FakeHttpResponse.create(IoUtils.stringFromResources(path), HTTP_OK);
    }

    private static FakeHttpResponse<String> mockIdentityServiceResponseForPublisherAllowingAutomaticPublishing() {
        return FakeHttpResponse.create(IoUtils.stringFromResources(Path.of("customer_allowing_publishing.json")),
                                       HTTP_OK);
    }

    private static FakeHttpResponse<String> mockIdentityServiceResponseCustomerAllowingPublishingMetadataOnly() {
        return FakeHttpResponse.create(
            IoUtils.stringFromResources(Path.of("customer_allowing_publishing_metadata_only.json")),
            HTTP_OK);
    }

    private static FakeHttpResponse<String> mockIdentityServiceResponseForCustomerRequiringApprovalForPublishing() {
        return FakeHttpResponse.create(
            IoUtils.stringFromResources(Path.of("customer_requires_approval_for_publishing.json")),
            HTTP_OK);
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

    private PublishingWorkflow getTicketPublishingWorkflow(Publication publication) {
        return getPublishingRequestCase(publication).getWorkflow();
    }

    private PublishingRequestCase getPublishingRequestCase(Publication publication) {
        return ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                             publication.getIdentifier(),
                                                             PublishingRequestCase.class).orElse(null);
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

    private void assertThatLocationHeaderPointsToCreatedTicket(URI ticketUri)
        throws NotFoundException {
        var publication = fetchPublication(ticketUri);
        var ticketIdentifier = extractTicketIdentifierFromLocation(ticketUri);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);
        assertThat(ticket.getResourceIdentifier(), is(equalTo(publication.getIdentifier())));
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

    private Publication createPersistedNonDegreePublishedPublication() throws ApiGatewayException {
        var publication = randomNonDegreePublication();
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
                   .withAuthorizerClaim(PERSON_AFFILIATION_CLAIM, userCredentials.getUsername())
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withUserName(userCredentials.getUsername())
                   .withCurrentCustomer(userCredentials.getCustomerId())
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(userCredentials.getTopLevelOrgCristinId())
                   .build();
    }

    private InputStream createHttpTicketCreationRequest(TicketDto ticketDto,
                                                        SortableIdentifier publicationIdentifier,
                                                        URI topLevelCristinOrganizationId,
                                                        URI userCristinId,
                                                        String username,
                                                        AccessRight... accessRights)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString()))
                   .withUserName(username)
                   .withCurrentCustomer(randomUri())
                   .withPersonCristinId(userCristinId)
                   .withTopLevelCristinOrgId(topLevelCristinOrganizationId)
                   .withAccessRights(randomUri(), accessRights)
                   .build();
    }

    private InputStream createHttpTicketCreationRequestWithAccessRight(TicketDto ticketDto,
                                                                 Publication publication, AccessRight... accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withAuthorizerClaim(PERSON_AFFILIATION_CLAIM, publication.getResourceOwner().getOwner().getValue())
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withUserName(publication.getResourceOwner().getOwner().getValue())
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withAccessRights(publication.getPublisher().getId(), accessRight)
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .build();
    }

    private InputStream createHttpTicketCreationRequestWithApprovedAccessRight(TicketDto ticketDto,
                                                                               Publication publication,
                                                                               URI customerId,
                                                                               AccessRight accessRight)
        throws JsonProcessingException {
        return createHttpTicketCreationRequestWithApprovedAccessRight(ticketDto,
                                                                      publication,
                                                                      randomString(),
                                                                      customerId,
                                                                      accessRight);
    }

    private InputStream createHttpTicketCreationRequestWithApprovedAccessRight(TicketDto ticketDto,
                                                                               Publication publication,
                                                                               String userName,
                                                                               URI customerId,
                                                                               AccessRight... accessRight)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<TicketDto>(JsonUtils.dtoObjectMapper)
                   .withBody(ticketDto)
                   .withAuthorizerClaim(PERSON_AFFILIATION_CLAIM, customerId.toString())
                   .withAccessRights(customerId, accessRight)
                   .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                   .withUserName(userName)
                   .withCurrentCustomer(customerId)
                   .withPersonCristinId(randomUri())
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
