package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.service.impl.UpdateResourceService.RESOURCE_WITHOUT_MAIN_TITLE_ERROR;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcceptedPublishingRequestEventHandlerTest extends ResourcesLocalTest {
    
    private static final Context CONTEXT = new FakeContext();
    private ResourceService resourceService;
    private AcceptedPublishingRequestEventHandler handler;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;
    
    @BeforeEach
    public void setup() {
        super.init();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        var s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, randomString());
        handler = new AcceptedPublishingRequestEventHandler(resourceService, s3Client);
        outputStream = new ByteArrayOutputStream();
        
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
        var event = createEvent(pendingPublishingRequest,approvedPublishingRequest);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
        assertThat(logger.getMessages(), containsString(RESOURCE_WITHOUT_MAIN_TITLE_ERROR));
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
    
    private Publication createUnpublishablePublication() throws ApiGatewayException {
        var publication = randomPublication();
        publication.getEntityDescription().setMainTitle(null);
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private PublishingRequestCase modifyPublishingRequest(PublishingRequestCase pendingPublishingRequest) {
        pendingPublishingRequest.setModifiedDate(randomInstant());
        return pendingPublishingRequest;
    }
    
    private Publication createPublication() throws ApiGatewayException {
        var publication = randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }
    
    private PublishingRequestCase pendingPublishingRequest(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        return PublishingRequestCase.createOpeningCaseObject(userInstance, publication.getIdentifier());
    }
}