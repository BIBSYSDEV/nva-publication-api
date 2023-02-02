package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler.EXPANDED_ENTRY_DELETE_EVENT_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DeleteResourceEvent;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

public class DeletePublicationEventConsumerTest {

    private ByteArrayOutputStream outputStream;
    private Context context;

    private DeletePublicationEventConsumer handler;

    private S3Client s3Client;

    @BeforeEach
    public void setUp() {
        s3Client = new FakeS3ClientSupportingDeleteObject();
        handler = new DeletePublicationEventConsumer(s3Client);
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
    }

    @Test
    void shouldReturnNullForNotDeletedPublication() throws IOException {
        var publication = randomPublication().copy()
                              .withIdentifier(SortableIdentifier.next())
                              .withDoi(null)
                              .withStatus(PublicationStatus.PUBLISHED).build();
        var event = createEventReference(publication);
        handler.handleRequest(event, outputStream, context);

        var response =
            objectMapper.readValue(outputStream.toString(), DeleteResourceEvent.class);
        assertThat(response, nullValue());
    }

    @Test
    void shouldReturnDeleteResourceEventWhenAPublicationWithStatusDeletedIsUpdated() throws IOException {
        var publication = randomPublication().copy()
                              .withIdentifier(SortableIdentifier.next())
                              .withStatus(PublicationStatus.DELETED)
                              .build();

        var event = createEventReference(publication);
        handler.handleRequest(event, outputStream, context);
        var response =
            objectMapper.readValue(outputStream.toString(), DeleteResourceEvent.class);
        assertThat(response.getIdentifier(), notNullValue());
        assertThat(response.getTopic(), is(equalTo(DeleteResourceEvent.EVENT_TOPIC)));
        assertThatDeleteObjectHasBeenDoneOnceWithCorrectObjectKey(publication);
    }

    private void assertThatDeleteObjectHasBeenDoneOnceWithCorrectObjectKey(Publication publication) {
        var fakeS3ClientSupportingDeleteObject = (FakeS3ClientSupportingDeleteObject) s3Client;
        var objectKeysRequestedForDeletion = fakeS3ClientSupportingDeleteObject.getDeletedObjectKeys();
        assertThat(objectKeysRequestedForDeletion, hasSize(1));
        assertThat(objectKeysRequestedForDeletion,
                   hasItem(equalTo("resources/" + publication.getIdentifier().toString())));
    }

    private InputStream createEventReference(Publication publication)
        throws IOException {

        var blobUri = createSampleBlob(publication);
        var event = new EventReference(EXPANDED_ENTRY_DELETE_EVENT_TOPIC, blobUri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(event);
    }

    private URI createSampleBlob(Publication publication) throws IOException {
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        return s3Driver.insertFile(filePath, publication.toString());
    }

    static class FakeS3ClientSupportingDeleteObject extends FakeS3Client {

        public List<String> deletedObjectKeys;

        public FakeS3ClientSupportingDeleteObject() {
            super();
            deletedObjectKeys = new ArrayList<>();
        }

        @Override
        public DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest) {
            deletedObjectKeys.add(deleteObjectRequest.key());
            return DeleteObjectResponse.builder().build();
        }

        public List<String> getDeletedObjectKeys() {
            return deletedObjectKeys;
        }
    }
}
