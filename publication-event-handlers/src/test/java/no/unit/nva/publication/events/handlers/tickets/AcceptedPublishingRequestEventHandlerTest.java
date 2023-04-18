package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
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

class AcceptedPublishingRequestEventHandlerTest extends ResourcesLocalTest {
    
    private static final Context CONTEXT = new FakeContext();
    public static final String RESOURCE_LACKS_REQUIRED_DATA = "Resource does not have required data to be "
                                                              + "published: ";
    private ResourceService resourceService;
    private TicketService ticketService;
    private AcceptedPublishingRequestEventHandler handler;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;
    
    @BeforeEach
    public void setup() {
        super.init();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
        var s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, randomString());
        handler = new AcceptedPublishingRequestEventHandler(resourceService, ticketService, s3Client);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldDoNothingWhenPublicationIsAlreadyPublished() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }
    
    @Test
    void shouldPublishPublicationWhenPublishingRequestIsApproved() throws ApiGatewayException, IOException {
        var publication = createPublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldPublishPublicationAndFilesWhenPublishingRequestIsApproved() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithUnpublishedFiles(PublicationStatus.DRAFT,
                                                                                         resourceService);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(getAssociatedFiles(updatedPublication), everyItem(instanceOf(PublishedFile.class)));
    }

    @Test
    void shouldCreateDoiRequestTicketWhenPublicationWithDraftDoiIsPublished()
        throws IOException, NotFoundException, BadRequestException {
        var publication = createDraftPublicationWithDoi();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket = ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                                   publication.getIdentifier(),
                                                                   DoiRequest.class);
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(ticket.get().getStatus(), is(equalTo(TicketStatus.PENDING)));
    }

    @Test
    void shouldNotCreateNewDoiRequestTicketWhenTicketAlreadyExists()
        throws NotFoundException, IOException, BadRequestException {
        var publication = createDraftPublicationWithDoi();
        var existingTicket = createDoiRequestTicket(publication);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var actualTicket = ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                                   publication.getIdentifier(),
                                                                   DoiRequest.class);
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(existingTicket.getIdentifier(), is(equalTo(actualTicket.get().getIdentifier())));
        assertThat(actualTicket.get().getResourceStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldNotPublishPublicationWhenPublishingRequestIsStillNotApproved() throws ApiGatewayException, IOException {
        var publication = createPublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var stillPendingPublishingRequest = modifyPublishingRequest(pendingPublishingRequest);
        var event = createEvent(pendingPublishingRequest, stillPendingPublishingRequest);
        
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
    }
    
    @Test
    void shouldLogFailingReasonsWhenPublishingFails() throws ApiGatewayException, IOException {
        var publication = createUnpublishablePublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
        assertThat(logger.getMessages(), containsString(RESOURCE_LACKS_REQUIRED_DATA));
    }

    private static List<AssociatedArtifact> getAssociatedFiles(Publication updatedPublication) {
        return updatedPublication.getAssociatedArtifacts().stream().filter(File.class::isInstance).collect(
            Collectors.toList());
    }

    private DoiRequest createDoiRequestTicket(Publication publication) {
        return (DoiRequest) attempt(
            () -> DoiRequest.fromPublication(publication).persistNewTicket(ticketService)).orElseThrow();
    }

    private Publication createDraftPublicationWithDoi() throws BadRequestException {
        var publication = randomPublication().copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withDoi(randomDoi())
                              .build();
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }

    private InputStream createEvent(TicketEntry pendingPublishingRequest,
                                    TicketEntry approvedPublishingRequest) throws IOException {
        var sampleEvent = eventBody(pendingPublishingRequest, approvedPublishingRequest);
        var eventReference = storeEventToS3AndGenerateEventReference(sampleEvent);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventReference);
    }
    
    private EventReference storeEventToS3AndGenerateEventReference(String sampleEvent) throws IOException {
        var blobUri = s3Driver.insertEvent(UnixPath.of(randomString()), sampleEvent);
        return new EventReference(DataEntryUpdateEvent.PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC, blobUri);
    }
    
    private String eventBody(TicketEntry pendingPublishingRequest,
                             TicketEntry approvedPublishingRequest) {
        return new DataEntryUpdateEvent(randomString(), pendingPublishingRequest, approvedPublishingRequest)
                   .toJsonString();
    }
    
    private Publication createUnpublishablePublication() throws BadRequestException {
        var publication = randomPublication();
        publication.getEntityDescription().setMainTitle(null);
        return Resource.fromPublication(publication).persistNew(resourceService,
            UserInstance.fromPublication(publication));
    }
    
    private PublishingRequestCase modifyPublishingRequest(PublishingRequestCase pendingPublishingRequest) {
        pendingPublishingRequest.setModifiedDate(randomInstant());
        return pendingPublishingRequest;
    }
    
    private Publication createPublication() throws BadRequestException {
        var publication = randomPublication();
        return Resource.fromPublication(publication).persistNew(resourceService,
            UserInstance.fromPublication(publication));
    }
    
    private PublishingRequestCase pendingPublishingRequest(Publication publication) {
        return PublishingRequestCase.createOpeningCaseObject(publication);
    }
}