package no.unit.nva.publication.events.handlers.tickets;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.*;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.http.FakeHttpResponse;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
class PendingPublishingRequestEventHandlerTest extends ResourcesLocalTest {
    
    public static final Entity EMPTY = null;
    private static final URI CUSTOMER_ID = randomUri();
    public static final String RESOURCE_LACKS_REQUIRED_DATA = "Resource does not have required data to be "
                                                              + "published: ";
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private PendingPublishingRequestEventHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;
    private ResourceService resourceService;
    private TicketService ticketService;

    @BeforeEach
    public void setup() {
        super.init();
        s3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(s3Client, randomString());
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }
    
    @Test
    void shouldApprovePublishingRequestAutomaticallyWhenCustomerHasPolicyAllowingRegistratorsToPublishDataAndMetadata()
        throws IOException, ApiGatewayException {
        var publishingRequest = pendingPublishingRequest();
        publishingRequest.setWorkflow(PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES);
        var event = createEvent(publishingRequest);
        this.handler = new PendingPublishingRequestEventHandler(resourceService, ticketService, s3Client);
        handler.handleRequest(event, output, context);
        var updatedPublishingRequest = ticketService.fetchTicket(publishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(TicketStatus.COMPLETED)));
    }
    
    @Test
    void shouldIgnorePublishingRequestWhenCustomerHasPolicyForbiddingRegistratorsToPublishDataAndMetadata()
        throws IOException, ApiGatewayException {
        var publishingRequest = pendingPublishingRequest();
        publishingRequest.setWorkflow(PublicationWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var event = createEvent(publishingRequest);

        this.handler = new PendingPublishingRequestEventHandler(resourceService, ticketService, s3Client);
        handler.handleRequest(event, output, context);
        var updatedPublishingRequest = ticketService.fetchTicket(publishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(TicketStatus.PENDING)));
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
        publishingRequest.setWorkflow(PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var event = createEvent(publishingRequest);

        this.handler = new PendingPublishingRequestEventHandler(resourceService, ticketService, s3Client);
        handler.handleRequest(event, output, context);
        var updatedPublishingRequest = ticketService.fetchTicket(publishingRequest);
        assertThat(updatedPublishingRequest.getStatus(), is(equalTo(TicketStatus.PENDING)));
        var resource = resourceService.getResourceByIdentifier(publishingRequest.getResourceIdentifier());
        assertThat(resource.getStatus(), is(equalTo(PublicationStatus.PUBLISHED_METADATA)));
        assertThatFilesAreUnpublished(resource);
    }

    @Test
    void shouldLogFailingReasonsWhenPublishingMetadataFails() throws ApiGatewayException, IOException {
        var publication = createUnpublishablePublication();
        var pendingPublishingRequest = PublishingRequestCase.createOpeningCaseObject(publication);
        pendingPublishingRequest.setWorkflow(PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var event = createEvent(pendingPublishingRequest);

        this.handler = new PendingPublishingRequestEventHandler(resourceService, ticketService, s3Client);

        var logger = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(event, output, context);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
        assertThat(logger.getMessages(), containsString(RESOURCE_LACKS_REQUIRED_DATA));
    }

    @Test
    void shouldLogFailingReasonsWhenUpdatingPublicationStatusFails() throws ApiGatewayException, IOException {
        var publication = createPublication();
        var pendingPublishingRequest = PublishingRequestCase.createOpeningCaseObject(publication);
        pendingPublishingRequest.setWorkflow(PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var event = createEvent(pendingPublishingRequest);

        var expectedMessage = "Testing string";
        var failingResourceService = mockResourceServiceFailure(expectedMessage);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        this.handler = new PendingPublishingRequestEventHandler(
            failingResourceService,
            ticketService,
            s3Client);
        handler.handleRequest(event, output, context);
        assertThat(logger.getMessages(), containsString(expectedMessage));
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
        return Resource.fromPublication(publication).persistNew(
            resourceService,
            UserInstance.fromPublication(publication));
    }

    private void assertThatFilesAreUnpublished(Resource resource) {
        var publishedFileCount = resource.getAssociatedArtifacts().stream()
                        .filter(PublishedFile.class::isInstance)
                        .count();
        assertThat(publishedFileCount, is(equalTo(0L)));
    }

    private void callApiOfCustomerAllowingAutomaticPublishing(PublishingRequestCase completedTicket)
            throws IOException {

        var event = createEvent(completedTicket);

        this.handler = new PendingPublishingRequestEventHandler(resourceService, ticketService, s3Client);
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

    private PublishingRequestCase createPendingPublishingRequest(Publication publication) throws ApiGatewayException {
        var publishingRequest = PublishingRequestCase.createOpeningCaseObject(publication);
        publishingRequest.setWorkflow(PublicationWorkflow.UNSET);
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