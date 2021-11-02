package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpandedEntryUpdateHandlerTest {

    private ExpandedEntryUpdateHandler handler;
    private S3Driver s3Reader;
    private S3Driver s3Writer;
    private URI eventUriInEventsBucket;
    private ByteArrayOutputStream output;
    private ResourceExpansionService resourceExpansionService;

    @BeforeEach
    public void init() throws IOException {
        var eventsBucket = new FakeS3Client();
        var indexBucket = new FakeS3Client();
        s3Reader = new S3Driver(eventsBucket, "eventsBucket");
        s3Writer = new S3Driver(indexBucket, "indexBucket");
        handler = new ExpandedEntryUpdateHandler(s3Reader, s3Writer);

        resourceExpansionService = fakeExpansionService();
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldEmitEventContainingContentS3UriAndIndexNameWhenInputIsResourceUpdate() throws IOException {
        var randomResource = randomResource();
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), randomResource.toJsonString());
        EventPayload outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getPayloadUri());
        assertThat(indexingEventPayload, is(not(emptyString())));
    }

    @Test
    void shouldEmitEventContainingContentS3UriAndIndexNameWhenInputIsDoiRequestUpdate() throws IOException {
        String content = randomDoiRequest();
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), content);
        EventPayload outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getPayloadUri());
        assertThat(indexingEventPayload, is(not(emptyString())));
    }

    @Test
    void shouldEmitEventContainingContentS3UriAndIndexNameWhenInputIsMessageUpdate() throws IOException {
        String content = randomMessage().toJsonString();
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), content);
        EventPayload outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getPayloadUri());
        assertThat(indexingEventPayload, is(not(emptyString())));
    }

    private EventPayload sendEvent() throws JsonProcessingException {
        EventPayload eventPayload = EventPayload.resourcesUpdateEvent(eventUriInEventsBucket);
        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventPayload);
        handler.handleRequest(event, output, mock(Context.class));
        return dynamoImageSerializerRemovingEmptyFields.readValue(output.toString(), EventPayload.class);
    }

    private Message randomMessage() {
        var randomUser = new UserInstance(randomString(), randomUri());
        var publication = PublicationGenerator.randomPublication();
        var clock = Clock.systemDefaultZone();
        return Message.supportMessage(randomUser, publication, randomString(), SortableIdentifier.next(), clock);
    }

    private ResourceExpansionServiceImpl fakeExpansionService() {
        return new ResourceExpansionServiceImpl(null, null) {
            @Override
            public Set<URI> getOrganizationIds(String username) {
                return Set.of(randomUri());
            }
        };
    }

    private ExpandedResource randomResource() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        return ExpandedResource.fromPublication(publication);
    }

    private String randomDoiRequest() {
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(
            Resource.fromPublication(PublicationGenerator.randomPublication()));
        ExpandedDoiRequest expandedDoiRequest = ExpandedDoiRequest.create(doiRequest, resourceExpansionService);
        return expandedDoiRequest.toJsonString();
    }
}