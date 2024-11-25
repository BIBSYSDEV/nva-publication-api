package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.ScanDatabaseRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.KeyField;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

class EventBasedBatchScanHandlerTest extends ResourcesLocalTest {

    public static final int LARGE_PAGE = 10;
    public static final int ONE_ENTRY_PER_EVENT = 1;
    public static final Map<String, AttributeValue> START_FROM_BEGINNING = null;
    public static final String OUTPUT_EVENT_TOPIC = "OUTPUT_EVENT_TOPIC";
    public static final String TOPIC = new Environment().readEnv(OUTPUT_EVENT_TOPIC);
    private static final String RESOURCES_TABLE_NAME = new Environment().readEnv("TABLE_NAME");
    private EventBasedBatchScanHandler handler;
    private ByteArrayOutputStream output;
    private FakeContext context;
    private FakeEventBridgeClient eventBridgeClient;
    private ResourceService resourceService;
    private TicketService ticketService;
    private AmazonDynamoDB dynamoDbClient;

    @Override
    @BeforeEach
    public void init() {
        super.init();

        this.output = new ByteArrayOutputStream();
        this.context = mockContent();
        this.eventBridgeClient = new FakeEventBridgeClient();
        dynamoDbClient = super.client;
        this.resourceService = spy(getResourceServiceBuilder().build());
        this.ticketService = getTicketService();
        this.handler = new EventBasedBatchScanHandler(resourceService, eventBridgeClient);
    }

    @Test
    void shouldUpdateDataEntriesWhenValidRequestIsReceived()
        throws ApiGatewayException {
        Publication createdPublication = createPublication(PublicationGenerator.randomPublication());
        Resource initialResource = resourceService.getResourceByIdentifier(createdPublication.getIdentifier());
        var originalDao = new ResourceDao(initialResource).fetchByIdentifier(client, RESOURCES_TABLE_NAME);
        handler.handleRequest(createInitialScanRequest(LARGE_PAGE), output, context);
        var updatedResource = resourceService.getResourceByIdentifier(createdPublication.getIdentifier());
        var updatedDao = new ResourceDao(initialResource).fetchByIdentifier(client, RESOURCES_TABLE_NAME);

        assertThat(updatedResource, is(equalTo(initialResource)));
        assertThat(updatedDao.getVersion(), is(not(equalTo(originalDao.getVersion()))));
    }

    @Test
    void shouldUpdateDataEntriesWithGivenTypeWhenRequestContainsType()
        throws ApiGatewayException {
        var createdPublication = createPublication(PublicationGenerator.randomPublication());
        var initialResource = resourceService.getResourceByIdentifier(createdPublication.getIdentifier());
        var originalDao = new ResourceDao(initialResource).fetchByIdentifier(client, RESOURCES_TABLE_NAME);
        var originalTicket = TicketEntry.requestNewTicket(createdPublication, PublishingRequestCase.class)
                                 .withOwner(UserInstance.fromPublication(createdPublication).getUsername())
                                 .persistNewTicket(ticketService);
        var originalTicketDao = fetchTicketDao(originalTicket.getIdentifier());

        handler.handleRequest(eventToInputStream(ScanDatabaseRequest.builder()
                                                     .withPageSize(LARGE_PAGE)
                                                     .withStartMarker(START_FROM_BEGINNING)
                                                     .withTopic(TOPIC)
                                                     .withTypes(List.of(KeyField.RESOURCE))
                                                     .build()), output, context);
        var updatedDao = new ResourceDao(initialResource).fetchByIdentifier(client, RESOURCES_TABLE_NAME);
        var updatedTicketDao = fetchTicketDao(originalTicket.getIdentifier());

        assertThat(updatedDao.getVersion(), is(not(equalTo(originalDao.getVersion()))));
        assertThat(updatedTicketDao.getVersion(), is(equalTo(originalTicketDao.getVersion())));
    }


    @Test
    void shouldUpdateTicketsWhenRequestContainsTicketKeyFieldOnly()
        throws ApiGatewayException {
        var createdPublication = createPublication(PublicationGenerator.randomPublication());
        var initialResource = resourceService.getResourceByIdentifier(createdPublication.getIdentifier());
        var originalDao = new ResourceDao(initialResource).fetchByIdentifier(client, RESOURCES_TABLE_NAME);
        var originalTicket = TicketEntry.requestNewTicket(createdPublication, PublishingRequestCase.class)
                                 .withOwner(UserInstance.fromPublication(createdPublication).getUsername())
                                 .persistNewTicket(ticketService);
        var originalTicketDao = fetchTicketDao(originalTicket.getIdentifier());

        handler.handleRequest(eventToInputStream(ScanDatabaseRequest.builder()
                                                     .withPageSize(LARGE_PAGE)
                                                     .withStartMarker(START_FROM_BEGINNING)
                                                     .withTopic(TOPIC)
                                                     .withTypes(List.of(KeyField.TICKET))
                                                     .build()), output, context);
        var updatedDao = new ResourceDao(initialResource).fetchByIdentifier(client, RESOURCES_TABLE_NAME);
        var updatedTicketDao = fetchTicketDao(originalTicket.getIdentifier());

        assertThat(updatedTicketDao.getVersion(), is(not(equalTo(originalTicketDao.getVersion()))));
        assertThat(updatedDao.getVersion(), is(equalTo(originalDao.getVersion())));
    }

    @Test
    void shouldUpdateDataEntriesDefaultTypesWhenRequestDoesNotContainType()
        throws ApiGatewayException {
        var createdPublication = createPublication(PublicationGenerator.randomPublication());
        var initialResource = resourceService.getResourceByIdentifier(createdPublication.getIdentifier());
        var originalDao = new ResourceDao(initialResource).fetchByIdentifier(client, RESOURCES_TABLE_NAME);
        var originalTicket = TicketEntry.requestNewTicket(createdPublication, PublishingRequestCase.class)
                                 .withOwner(UserInstance.fromPublication(createdPublication).getUsername())
                                 .persistNewTicket(ticketService);
        var originalTicketDao = fetchTicketDao(originalTicket.getIdentifier());

        handler.handleRequest(eventToInputStream(ScanDatabaseRequest.builder()
                                                     .withPageSize(LARGE_PAGE)
                                                     .withStartMarker(START_FROM_BEGINNING)
                                                     .withTopic(TOPIC)
                                                     .build()), output, context);
        var updatedDao = new ResourceDao(initialResource).fetchByIdentifier(client, RESOURCES_TABLE_NAME);
        var updatedTicketDao = fetchTicketDao(originalTicket.getIdentifier());

        assertThat(updatedDao.getVersion(), is(not(equalTo(originalDao.getVersion()))));
        assertThat(updatedTicketDao.getVersion(), is(not(equalTo(originalTicketDao.getVersion()))));
    }

    @Test
    void shouldEmitNewScanEventWhenDatabaseScanningIsNotComplete() throws ApiGatewayException {
        createRandomResources(2);
        handler.handleRequest(createInitialScanRequest(ONE_ENTRY_PER_EVENT), output, context);
        var emittedEvent = consumeLatestEmittedEvent();
        assertThat(emittedEvent.getStartMarker(), is(not(nullValue())));
    }

    @Test
    void shouldNotSendScanEventWhenDatabaseScanningIsComplete()
        throws ApiGatewayException {
        createRandomResources(2);
        handler.handleRequest(createInitialScanRequest(LARGE_PAGE), output, context);
        assertThat(eventBridgeClient.getRequestEntries(), is(empty()));
    }

    @Test
    void shouldStartScanningFromSuppliedStartMarkerWhenStartMakerIsNotNull()
        throws ApiGatewayException {
        createRandomResources(5);
        handler.handleRequest(createInitialScanRequest(ONE_ENTRY_PER_EVENT), output, context);

        var expectedStaringPointForNextEvent = getLatestEmittedStartingPoint();
        var secondScanRequest = ScanDatabaseRequest.builder()
                                    .withPageSize(ONE_ENTRY_PER_EVENT)
                                    .withStartMarker(expectedStaringPointForNextEvent)
                                    .withTopic(TOPIC)
                                    .build();
        handler.handleRequest(eventToInputStream(secondScanRequest), output, context);

        var startingPointsCapturer = ArgumentCaptor.forClass(Map.class);
        verify(resourceService, atLeastOnce()).scanResources(anyInt(), startingPointsCapturer.capture(), any());
        var scanStartingPointSentToTheService = startingPointsCapturer.getValue();

        assertThat(scanStartingPointSentToTheService, is(equalTo(expectedStaringPointForNextEvent)));
    }

    @Test
    void shouldNotGoIntoInfiniteLoop() throws ApiGatewayException {
        createRandomResources(20);
        pushInitialEntryInEventBridge(ScanDatabaseRequest.builder()
                                          .withPageSize(ONE_ENTRY_PER_EVENT)
                                          .withStartMarker(START_FROM_BEGINNING)
                                          .withTopic(TOPIC)
                                          .build());
        while (thereAreMoreEventsInEventBridge()) {
            var currentRequest = consumeLatestEmittedEvent();
            handler.handleRequest(eventToInputStream(currentRequest), output, context);
        }
        assertThat(eventBridgeClient.getRequestEntries(), is(empty()));
    }

    @Test
    void shouldLogFailureWhenExceptionIsThrown() {
        final var logger = LogUtils.getTestingAppenderForRootLogger();
        var expectedExceptionMessage = randomString();
        var spiedResourceService = resourceService;
        doThrow(new RuntimeException(expectedExceptionMessage)).when(spiedResourceService)
            .scanResources(anyInt(), any(), any());

        handler = new EventBasedBatchScanHandler(spiedResourceService, eventBridgeClient);
        Executable action = () -> handler.handleRequest(createInitialScanRequest(ONE_ENTRY_PER_EVENT), output, context);
        assertThrows(RuntimeException.class, action);
        assertThat(logger.getMessages(), containsString(expectedExceptionMessage));
    }

    private TicketDao fetchTicketDao(SortableIdentifier identifier) throws NotFoundException {
        var queryObject = TicketEntry.createQueryObject(identifier);
        var queryResult = queryObject.fetchByIdentifier(dynamoDbClient, RESOURCES_TABLE_NAME);
        return (TicketDao) queryResult;
    }

    private void createRandomResources(int numberOfResources) throws ApiGatewayException {
        for (int i = 0; i < numberOfResources; i++) {
            createPublication(PublicationGenerator.randomPublication());
        }
    }

    private Publication createPublication(Publication publication) throws ApiGatewayException {
        UserInstance userInstance = UserInstance.fromPublication(publication);
        return Resource.fromPublication(publication).persistNew(resourceService, userInstance);
    }

    private FakeContext mockContent() {
        return new FakeContext() {
            @Override
            public String getInvokedFunctionArn() {
                return randomString();
            }
        };
    }

    private void pushInitialEntryInEventBridge(ScanDatabaseRequest initialRequest) {
        var entry = PutEventsRequestEntry.builder()
                        .detail(initialRequest.toJsonString())
                        .build();
        eventBridgeClient.getRequestEntries().add(entry);
    }

    private boolean thereAreMoreEventsInEventBridge() {
        return !eventBridgeClient.getRequestEntries().isEmpty();
    }

    private Map<String, AttributeValue> getLatestEmittedStartingPoint() {
        return consumeLatestEmittedEvent().getStartMarker();
    }

    private ScanDatabaseRequest consumeLatestEmittedEvent() {
        var allRequests = eventBridgeClient.getRequestEntries();
        var latest = allRequests.removeLast();
        return attempt(() -> ScanDatabaseRequest.fromJson(latest.detail())).orElseThrow();
    }

    private InputStream createInitialScanRequest(int pageSize) {
        return eventToInputStream(ScanDatabaseRequest.builder()
                                      .withPageSize(pageSize)
                                      .withStartMarker(START_FROM_BEGINNING)
                                      .withTopic(TOPIC)
                                      .build());
    }

    private InputStream eventToInputStream(ScanDatabaseRequest scanDatabaseRequest) {
        var event = new AwsEventBridgeEvent<ScanDatabaseRequest>();
        event.setAccount(randomString());
        event.setVersion(randomString());
        event.setSource(randomString());
        event.setRegion(randomElement(Region.regions()));
        event.setDetail(scanDatabaseRequest);
        return IoUtils.stringToStream(event.toJsonString());
    }
}