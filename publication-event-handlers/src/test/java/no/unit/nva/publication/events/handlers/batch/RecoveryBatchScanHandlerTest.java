package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.IntStream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.handlers.recovery.RecoveryBatchScanHandler;
import no.unit.nva.publication.events.handlers.recovery.RecoveryEventRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.FakeSqsClient;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class RecoveryBatchScanHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = Mockito.mock(Context.class);
    private ByteArrayOutputStream outputStream;
    private ResourceService resourceService;
    private FakeSqsClient queueClient;
    private FakeEventBridgeClient eventBridgeClient;
    private RecoveryBatchScanHandler recoveryBatchScanHandler;

    @BeforeEach
    public void init() {
        super.init();
        outputStream = new ByteArrayOutputStream();
        resourceService = getResourceServiceBuilder().build();
        queueClient = new FakeSqsClient();
        eventBridgeClient = new FakeEventBridgeClient();
        recoveryBatchScanHandler = new RecoveryBatchScanHandler(resourceService, queueClient, eventBridgeClient);
    }

    @Test
    void shouldUpdateResourceVersionByReadingQueueMessageContainingResourceIdentifierWhenResourceIsPublication()
        throws JsonProcessingException, NotFoundException {
        var publication = persistedPublication();
        var resourceVersion = Resource.fromPublication(publication).toDao().getVersion();
        putMessageOnRecoveryQueue(publication.getIdentifier());
        recoveryBatchScanHandler.handleRequest(createEvent(), outputStream, CONTEXT);

        var refreshedPublication = resourceService.getPublication(publication);
        var resourceVersionAfterRefresh = Resource.fromPublication(refreshedPublication).toDao().getVersion();

        assertThat(resourceVersionAfterRefresh, is(not(equalTo(resourceVersion))));
    }

    @Test
    void shouldRemoveMessageFromQueueAfterItHasBeenRefreshed() throws JsonProcessingException {
        var publication = persistedPublication();
        putMessageOnRecoveryQueue(publication.getIdentifier());
        recoveryBatchScanHandler.handleRequest(createEvent(), outputStream, CONTEXT);

        assertTrue(queueClient.getDeliveredMessages().isEmpty());
    }

    @Test
    void shouldEmitNewEventWhenThereAreMoreEntriesOnRecoverQueue() throws JsonProcessingException {
        persistPublicationsAndPutMessagesOnQueue(1);

        recoveryBatchScanHandler.handleRequest(createEvent(), outputStream, CONTEXT);
        var emittedEvents = eventBridgeClient.getRequestEntries();

        assertFalse(emittedEvents.isEmpty());
    }

    @Test
    void shouldNotEmitNewEventWhenNoMessagesOnQueue() throws JsonProcessingException {
        recoveryBatchScanHandler.handleRequest(createEvent(), outputStream, CONTEXT);
        var emittedEvents = eventBridgeClient.getRequestEntries();

        assertTrue(emittedEvents.isEmpty());
    }

    private static InputStream createEvent() throws JsonProcessingException {
        var event = new AwsEventBridgeEvent<RecoveryEventRequest>();
        event.setId(randomString());
        var jsonString = JsonUtils.dtoObjectMapper.writeValueAsString(event);
        return IoUtils.stringToStream(jsonString);
    }

    //TODO: Implement recovery for other entity types than publication

    private static MessageAttributeValue messageAttribute(String value) {
        return MessageAttributeValue.builder().stringValue(value).dataType("String").build();
    }

    private void persistPublicationsAndPutMessagesOnQueue(int numberOfPublications) {
        IntStream.range(0, numberOfPublications)
            .boxed()
            .map(i -> persistedPublication())
            .forEach(publication -> putMessageOnRecoveryQueue(publication.getIdentifier()));
    }

    private void putMessageOnRecoveryQueue(SortableIdentifier identifier) {
        var id = Map.of("id", messageAttribute(identifier.toString()), "type", messageAttribute(""));
        queueClient.sendMessage(SendMessageRequest.builder().queueUrl(randomString()).messageAttributes(id).build());
    }

    private Publication persistedPublication() {
        var publication = randomPublication();
        return attempt(() -> resourceService.createPublication(UserInstance.fromPublication(publication),
                                                               publication)).orElseThrow();
    }
}