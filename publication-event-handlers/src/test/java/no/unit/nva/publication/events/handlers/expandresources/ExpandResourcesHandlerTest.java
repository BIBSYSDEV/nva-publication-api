package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Path;
import no.unit.nva.events.handlers.EventParser;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExpandResourcesHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final int SINGLE_EXPECTED_FILE = 0;
    public static final String EVENT_WITH_NEW_PUBLISHED_RESOURCE = stringFromResources(
        Path.of("expandResources/sample-event-old-is-draft-new-is-published.json"));
    private ByteArrayOutputStream output;
    private ExpandResourcesHandler expandResourceHandler;
    private S3Driver s3Driver;

    @BeforeEach
    public void init() {
        this.output = new ByteArrayOutputStream();
        FakeS3Client s3Client = new FakeS3Client();
        this.expandResourceHandler = new ExpandResourcesHandler(s3Client);
        this.s3Driver = new S3Driver(s3Client, "ignoredForFakeS3Client");
    }

    @Test
    void shouldSaveTheNewestResourceImageInS3WhenThereIsNewResourceImagePresentInTheEventAndIsNotDraftResource()
        throws JsonProcessingException {
        expandResourceHandler.handleRequest(stringToStream(EVENT_WITH_NEW_PUBLISHED_RESOURCE), output, CONTEXT);
        var allFiles = s3Driver.listAllFiles(UnixPath.ROOT_PATH);
        assertThat(allFiles.size(), is(equalTo(1)));
        var contents = s3Driver.getFile(allFiles.get(SINGLE_EXPECTED_FILE));
        var documentToIndex = dynamoImageSerializerRemovingEmptyFields.readValue(contents, Publication.class);

        ResourceUpdate actualResource = Resource.fromPublication(documentToIndex);
        ResourceUpdate expectedImage = extractResourceUpdateFromEvent(EVENT_WITH_NEW_PUBLISHED_RESOURCE);
        assertThat(actualResource, is(equalTo(expectedImage)));
    }

    @Test
    void shouldEmitEventThatContainsTheEventPayloadS3Uri()
        throws JsonProcessingException {
        expandResourceHandler.handleRequest(stringToStream(EVENT_WITH_NEW_PUBLISHED_RESOURCE), output, CONTEXT);
        var updateEvent = parseEmittedEvent();
        var uriWithEventPayload = updateEvent.getPayloadUri();
        var actualResourceUpdate = fetchResourceUpdateFromS3(uriWithEventPayload);
        var expectedResourceUpdate = extractResourceUpdateFromEvent(EVENT_WITH_NEW_PUBLISHED_RESOURCE);
        assertThat(actualResourceUpdate, is(equalTo(expectedResourceUpdate)));
    }

    private ResourceUpdate fetchResourceUpdateFromS3(URI uriWithEventPayload) throws JsonProcessingException {
        var resourceUpdateString = s3Driver.getFile(new UriWrapper(uriWithEventPayload).toS3bucketPath());
        Publication publication =
            dynamoImageSerializerRemovingEmptyFields.readValue(resourceUpdateString,Publication.class);
        return Resource.fromPublication(publication);
    }

    private EventPayload parseEmittedEvent() throws JsonProcessingException {
        return dynamoImageSerializerRemovingEmptyFields.readValue(output.toString(), EventPayload.class);
    }

    private ResourceUpdate extractResourceUpdateFromEvent(String eventString) {
        var event = parseEvent(eventString);
        return event.getDetail().getResponsePayload().getNewData();
    }

    @SuppressWarnings("unchecked")
    private AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> parseEvent(String eventString) {
        return (AwsEventBridgeEvent<AwsEventBridgeDetail<DynamoEntryUpdateEvent>>)
                   newEventParser(eventString).parse(AwsEventBridgeDetail.class, DynamoEntryUpdateEvent.class);
    }

    private EventParser<AwsEventBridgeDetail<DynamoEntryUpdateEvent>> newEventParser(String eventString) {
        return new EventParser<>(eventString, dynamoImageSerializerRemovingEmptyFields);
    }
}
