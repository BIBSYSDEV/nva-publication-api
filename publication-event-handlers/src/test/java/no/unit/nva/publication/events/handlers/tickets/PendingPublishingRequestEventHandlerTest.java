package no.unit.nva.publication.events.handlers.tickets;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.tickets.PendingPublishingRequestEventHandler.BACKEND_CLIENT_SECRET_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.Entity;
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
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PendingPublishingRequestEventHandlerTest extends ResourcesLocalTest {

    public static final Entity EMPTY = null;
    public static final String RESOURCE_LACKS_REQUIRED_DATA = "Resource does not have required data to be "
                                                              + "published: ";
    private static final URI CUSTOMER_ID = randomUri();
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private PendingPublishingRequestEventHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;
    private ResourceService resourceService;
    private TicketService ticketService;
    private FakeHttpClient<String> httpClient;
    private SecretsReader secretsReader;

    @BeforeEach
    public void setup() {
        super.init();
        s3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(s3Client, randomString());
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);

        var secretManagerClient = new FakeSecretsManagerClient();
        var credentials = new BackendClientCredentials("id", "secret");
        secretManagerClient.putPlainTextSecret(BACKEND_CLIENT_SECRET_NAME, credentials.toString());
        this.secretsReader = new SecretsReader(secretManagerClient);

        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }

    @Test
    void shouldDoNothingWhenPublicationIsAlreadyPublished()
        throws IOException, ApiGatewayException {
        var publishingRequest = completedPublishingRequest();
        var event = createEvent(publishingRequest);
        var customerAllowingPublishing =
            mockIdentityServiceResponseForPublisherAllowingAutomaticPublishingRequestsApproval();
        this.httpClient = new FakeHttpClient<>(FakeHttpResponse.create(customerAllowingPublishing, HTTP_OK));

        this.handler = new PendingPublishingRequestEventHandler(resourceService, httpClient,
                                                                secretsReader, s3Client);
        handler.handleRequest(event, output, context);
        var updatedPublishingRequest = ticketService.fetchTicket(publishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(TicketStatus.COMPLETED)));
    }

    @Test
    void shouldDoNothingWhenMetadataIsAlreadyPublished() throws ApiGatewayException, IOException {
        var publishingRequest = completedPublishingRequest();
        var event = createEvent(publishingRequest);
        var customerAllowingMetadataPublishing =
            mockIdentityServiceResponseForPublisherAllowingMetadataPublishing();
        this.httpClient = new FakeHttpClient<>(FakeHttpResponse.create(customerAllowingMetadataPublishing, HTTP_OK));
        this.handler = new PendingPublishingRequestEventHandler(resourceService, httpClient,
                                                                secretsReader, s3Client);
        handler.handleRequest(event, output, context);
        var updatedPublishingRequest = ticketService.fetchTicket(publishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(TicketStatus.COMPLETED)));
    }

    @Test
    void shouldIgnorePublishingRequestWhenCustomerHasPolicyForbiddingRegistratorsToPublishDataAndMetadata()
        throws IOException, ApiGatewayException {
        var publishingRequest = pendingPublishingRequest();
        var event = createEvent(publishingRequest);

        var customerAllowingPublishing =
            mockIdentityServiceResponseForCustomersThatRequireManualApprovalOfPublishingRequests();
        this.httpClient = new FakeHttpClient<>(FakeHttpResponse.create(customerAllowingPublishing, HTTP_OK));

        this.handler = new PendingPublishingRequestEventHandler(resourceService, httpClient,
                                                                secretsReader, s3Client);
        handler.handleRequest(event, output, context);
        var updatedPublishingRequest = ticketService.fetchTicket(publishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(TicketStatus.PENDING)));
    }

    @Test
    void shouldIgnorePublishingRequestAndLogResponseWhenCustomerCannotBeResolved()
        throws IOException, ApiGatewayException {
        var publishingRequest = pendingPublishingRequest();
        var event = createEvent(publishingRequest);
        final var logger = LogUtils.getTestingAppenderForRootLogger();

        var tokenResponse = tokenResponse();
        var identityServiceResponse = unresolvableCustomer();
        this.httpClient = new FakeHttpClient<>(tokenResponse, identityServiceResponse, tokenResponse);
        this.handler = new PendingPublishingRequestEventHandler(resourceService, httpClient,
                                                                secretsReader, s3Client);

        handler.handleRequest(event, output, context);
        var updatedPublishingRequest = ticketService.fetchTicket(publishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(TicketStatus.PENDING)));
        assertThat(logger.getMessages(), containsString(identityServiceResponse.body()));
    }

    @Test
    void shouldNotCompleteAlreadyCompletedTicketsAndEnterInfiniteLoop() throws ApiGatewayException, IOException {
        var completedTicket = createCompletedTicketEntry();
        var versionBeforeEvent = completedTicket.toDao().fetchByIdentifier(client).getVersion();
        callApiOfCustomerAllowingAutomaticPublishing((PublishingRequestCase) completedTicket);
        var versionAfterEvent = completedTicket.toDao().fetchByIdentifier(client).getVersion();
        assertThat(versionAfterEvent, is(equalTo(versionBeforeEvent)));
    }

    @Test
    void shouldUpdatePublicationStatusButNotCompleteTicketWhenCustomerAllowsMetadataPublishing()
        throws ApiGatewayException, IOException {
        var publishingRequest = pendingPublishingRequest();
        var event = createEvent(publishingRequest);
        var customerAllowingMetadataPublishing =
            mockIdentityServiceResponseForPublisherAllowingMetadataPublishing();
        this.httpClient = new FakeHttpClient<>(FakeHttpResponse.create(customerAllowingMetadataPublishing, HTTP_OK));

        this.handler = new PendingPublishingRequestEventHandler(resourceService, httpClient,
                                                                secretsReader, s3Client);
        handler.handleRequest(event, output, context);
        var updatedPublishingRequest = ticketService.fetchTicket(publishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(TicketStatus.PENDING)));
        var resource = resourceService.getResourceByIdentifier(publishingRequest.getResourceIdentifier());
        assertThat(resource.getStatus(), is(equalTo(PublicationStatus.PUBLISHED_METADATA)));
    }

    @Test
    void shouldLogFailingReasonsWhenPublishingMetadataFails() throws ApiGatewayException, IOException {
        var publication = createUnpublishablePublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var event = createEvent(pendingPublishingRequest);

        this.httpClient = new FakeHttpClient<>(FakeHttpResponse
                                                   .create(
                                                       mockIdentityServiceResponseForPublisherAllowingMetadataPublishing(),
                                                       HTTP_OK));
        this.handler = new PendingPublishingRequestEventHandler(resourceService, httpClient,
                                                                secretsReader, s3Client);

        var logger = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(event, output, context);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
        assertThat(logger.getMessages(), containsString(RESOURCE_LACKS_REQUIRED_DATA));
    }

    @Test
    void shouldLogFailingReasonsWhenUpdatingPublicationStatusFails() throws ApiGatewayException, IOException {
        var publication = createPublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var event = createEvent(pendingPublishingRequest);
        this.httpClient = new FakeHttpClient<>(FakeHttpResponse
                                                   .create(
                                                       mockIdentityServiceResponseForPublisherAllowingMetadataPublishing(),
                                                       HTTP_OK));

        var expectedMessage = "Testing string";
        var failingResourceService = mockResourceServiceFailure(expectedMessage);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        this.handler = new PendingPublishingRequestEventHandler(failingResourceService,
                                                                httpClient,
                                                                secretsReader,
                                                                s3Client);
        handler.handleRequest(event, output, context);
        assertThat(logger.getMessages(), containsString(expectedMessage));
    }

    private static String mockIdentityServiceResponseForCustomersThatRequireManualApprovalOfPublishingRequests() {
        return IoUtils.stringFromResources(Path.of("publishingrequests", "customers",
                                                   "customer_forbidding_publishing.json"));
    }

    private static FakeHttpResponse<String> unresolvableCustomer() {
        return FakeHttpResponse.create(randomString(), HTTP_NOT_FOUND);
    }

    private static FakeHttpResponse<String> tokenResponse() {
        return FakeHttpResponse.create("{ \"access_token\" : \"Bearer token\"}", HTTP_OK);
    }

    private static String mockIdentityServiceResponseForPublisherAllowingAutomaticPublishingRequestsApproval() {
        return IoUtils.stringFromResources(Path.of("publishingrequests", "customers",
                                                   "customer_allowing_publishing.json"));
    }

    private static String mockIdentityServiceResponseForPublisherAllowingMetadataPublishing() {
        return IoUtils.stringFromResources(Path.of("publishingrequests", "customers",
                                                   "customer_allowing_metadata_publishing.json"));
    }

    private ResourceService mockResourceServiceFailure(String expectedMessage) throws ApiGatewayException {
        var resourceServiceMock = mock(ResourceService.class);
        when(resourceServiceMock.publishPublicationMetadata(any(), any()))
            .thenThrow(new NotFoundException(expectedMessage));
        return resourceServiceMock;
    }

    private Publication createUnpublishablePublication() throws BadRequestException {
        var publication = randomPublication();
        publication.getEntityDescription().setMainTitle(null);
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }

    private void callApiOfCustomerAllowingAutomaticPublishing(PublishingRequestCase completedTicket)
        throws IOException {
        var customerAllowingPublishing =
            mockIdentityServiceResponseForPublisherAllowingAutomaticPublishingRequestsApproval();

        var event = createEvent(completedTicket);

        this.httpClient = new FakeHttpClient<>(FakeHttpResponse.create(customerAllowingPublishing, HTTP_OK));
        this.handler = new PendingPublishingRequestEventHandler(resourceService, httpClient,
                                                                secretsReader, s3Client);
        handler.handleRequest(event, output, context);
    }

    private TicketEntry createCompletedTicketEntry() throws ApiGatewayException {
        var publication = createPublication();
        var completedTicket = TicketEntry.requestNewTicket(publication, PublishingRequestCase.class)
                                  .persistNewTicket(ticketService)
                                  .complete(publication);
        completedTicket = ticketService.updateTicketStatus(completedTicket, TicketStatus.COMPLETED);
        return completedTicket;
    }

    private InputStream createEvent(PublishingRequestCase publishingRequest) throws IOException {
        var blobUri = createEventBlob(publishingRequest);
        var event = new EventReference(DataEntryUpdateEvent.PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC, blobUri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(event);
    }

    private URI createEventBlob(PublishingRequestCase publishingRequest) throws IOException {
        var dataEntryUpdateEvent = firstAppearanceOfAPublishingRequest(publishingRequest);
        var blobContent = JsonUtils.dtoObjectMapper.writeValueAsString(dataEntryUpdateEvent);
        return s3Driver.insertEvent(UnixPath.of(randomString()), blobContent);
    }

    private DataEntryUpdateEvent firstAppearanceOfAPublishingRequest(PublishingRequestCase publishingRequest) {
        return new DataEntryUpdateEvent(randomString(), EMPTY, publishingRequest);
    }

    private PublishingRequestCase pendingPublishingRequest() throws ApiGatewayException {
        var publication = createPublication();
        return createPendingPublishingRequest(publication);
    }

    private PublishingRequestCase completedPublishingRequest() throws ApiGatewayException {
        var publication = createPublication();
        return createCompletedPublishingRequest(publication);
    }

    private PublishingRequestCase createCompletedPublishingRequest(Publication publication) throws ApiGatewayException {
        var publishingRequest = TicketTestUtils.createCompletedTicket(publication, PublishingRequestCase.class,
                                                                      ticketService);
        return (PublishingRequestCase) publishingRequest;
    }

    private PublishingRequestCase pendingPublishingRequest(Publication publication) {
        return PublishingRequestCase.createOpeningCaseObject(publication);
    }

    private PublishingRequestCase createPendingPublishingRequest(Publication publication) throws ApiGatewayException {
        var publishingRequest = PublishingRequestCase.createOpeningCaseObject(publication);
        return (PublishingRequestCase) publishingRequest.persistNewTicket(ticketService);
    }

    private Publication createPublication() throws BadRequestException {
        var publication = randomPublication();
        publication.setStatus(PublicationStatus.DRAFT);
        publication.setPublisher(new Organization.Builder().withId(CUSTOMER_ID).build());
        publication = Resource.fromPublication(publication)
                          .persistNew(resourceService, UserInstance.fromPublication(publication));
        return publication;
    }
}