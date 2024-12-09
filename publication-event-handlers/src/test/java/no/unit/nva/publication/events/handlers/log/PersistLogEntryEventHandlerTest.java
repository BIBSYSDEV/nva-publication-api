package no.unit.nva.publication.events.handlers.log;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.RESOURCE_UPDATE_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import no.unit.nva.clients.GetCustomerResponse;
import no.unit.nva.clients.GetUserResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersistLogEntryEventHandlerTest extends ResourcesLocalTest {

    private PersistLogEntryEventHandler handler;
    private ResourceService resourceService;
    private FakeS3Client s3Client;
    private ByteArrayOutputStream outputStream;
    private Context context;
    private IdentityServiceClient identityServiceClient;

    @BeforeEach
    public void setUp() throws NotFoundException {
        super.init();
        outputStream = new ByteArrayOutputStream();
        context = null;
        s3Client = new FakeS3Client();
        resourceService = getResourceServiceBuilder().build();
        identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getUser(any())).thenReturn(randomUser());
        when(identityServiceClient.getCustomerByCristinId(any())).thenReturn(randomCustomer());
        handler = new PersistLogEntryEventHandler(s3Client, resourceService, identityServiceClient);
    }

    @Test
    void shouldCreateLogEntryWhenConsumedEventHasResourceWithNewImageWhereResourceEventIsPresent()
        throws BadRequestException, IOException {
        var publication = createPublication();
        var event = createEvent(null, publication);

        handler.handleRequest(event, outputStream, context);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertFalse(logEntries.isEmpty());
    }

    @Test
    void shouldCreateLogEntryWithUserUsernameOnlyWhenFailingWhenFetchingUser()
        throws BadRequestException, IOException, NotFoundException {
        var publication = createPublication();
        when(identityServiceClient.getUser(any())).thenThrow(new NotFoundException("User not found"));
        var event = createEvent(null, publication);

        handler.handleRequest(event, outputStream, context);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        var logUser = logEntries.getFirst().performedBy();
        assertNotNull(logUser.userName());
        assertNull(logUser.cristinId());
    }

    @Test
    void shouldNotCreateLogEntryWhenConsumedEventHasResourceWithNewImageWhereResourceEventIsNull()
        throws BadRequestException, IOException {
        var publication = createPublication();
        Resource.fromPublication(publication).clearResourceEvent(resourceService);
        var event = createEvent(null, publication);

        handler.handleRequest(event, outputStream, context);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertTrue(logEntries.isEmpty());
    }

    @Test
    void shouldNotFailWhenWhenConsumedEventIsMissingNewImage() throws IOException {
        var event = createEvent(randomPublication(), null);

        assertDoesNotThrow(() -> handler.handleRequest(event, outputStream, context));
    }

    private Publication createPublication() throws BadRequestException {
        var publication = randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }

    private InputStream createEvent(Publication oldImage, Publication newImage) throws IOException {
        var dataEntryUpdateEvent = new DataEntryUpdateEvent(RESOURCE_UPDATE_EVENT_TOPIC,
                                                            Resource.fromPublication(oldImage),
                                                            Resource.fromPublication(newImage));
        var uri = new S3Driver(s3Client, EVENTS_BUCKET).insertEvent(UnixPath.of(UUID.randomUUID().toString()),
                                                                    dataEntryUpdateEvent.toJsonString());
        var eventReference = new EventReference(RESOURCE_UPDATE_EVENT_TOPIC, uri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventReference);
    }

    private GetUserResponse randomUser() {
        return GetUserResponse.builder()
                   .withInstitutionCristinId(randomUri())
                   .withFamilyName(randomString())
                   .withGivenName(randomString())
                   .withCristinId(randomUri())
                   .build();
    }

    private GetCustomerResponse randomCustomer() {
        return new GetCustomerResponse(randomUri(), UUID.randomUUID(), randomString(), randomString(), randomString(),
                                       randomUri());
    }
}