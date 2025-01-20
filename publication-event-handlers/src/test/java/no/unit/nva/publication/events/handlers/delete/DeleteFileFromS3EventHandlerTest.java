package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.FILE_ENTRY_DELETE_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class DeleteFileFromS3EventHandlerTest {

    private static final Context CONTEXT = new FakeContext();
    private DeleteFileFromS3EventHandler handler;
    private FakeS3Client s3Client;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void setUp() {
        output = new ByteArrayOutputStream();
        s3Client = new FakeS3Client();
        handler = new DeleteFileFromS3EventHandler(s3Client);
    }

    @Test
    void shouldDeleteFileFromS3WhenFileEntryHasBeenDeleted() throws IOException {
        var fileEntry = FileEntry.create(randomOpenFile(), SortableIdentifier.next(),
                                         UserInstance.create(randomString(), randomUri()));

        var persistedStorageS3Driver = new S3Driver(s3Client,
                                                    new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME"));
        persistedStorageS3Driver.insertEvent(UnixPath.of(fileEntry.getIdentifier().toString()), randomString());
        insertFile(fileEntry, persistedStorageS3Driver);
        var event = createEvent(fileEntry, null);
        handler.handleRequest(event, output, CONTEXT);

        assertThrows(NoSuchKeyException.class,
                     () -> persistedStorageS3Driver.getFile(UnixPath.of(fileEntry.getIdentifier().toString())));
    }

    @Test
    void shouldDoNothingWhenEventIsNotDeleteEvent() throws IOException {
        var fileEntry = FileEntry.create(randomOpenFile(), SortableIdentifier.next(),
                                         UserInstance.create(randomString(), randomUri()));

        var persistedStorageS3Driver = new S3Driver(s3Client, new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME"));
        var fileKey = insertFile(fileEntry, persistedStorageS3Driver);

        var event = createEvent(fileEntry, fileEntry);

        handler.handleRequest(event, output, CONTEXT);

        assertNotNull(persistedStorageS3Driver.getFile(UnixPath.of(fileKey)));
    }

    private static String insertFile(FileEntry fileEntry, S3Driver persistedStorageS3Driver) throws IOException {
        var s3Key = fileEntry.getIdentifier().toString();
        persistedStorageS3Driver.insertFile(UnixPath.of(s3Key), randomString());
        return s3Key;
    }

    private InputStream createEvent(FileEntry oldImage, FileEntry newImage) throws IOException {
        var dataEntryUpdateEvent = new DataEntryUpdateEvent(FILE_ENTRY_DELETE_EVENT_TOPIC, oldImage, newImage);
        var uri = new S3Driver(s3Client, EVENTS_BUCKET).insertEvent(UnixPath.of(UUID.randomUUID().toString()),
                                                                    dataEntryUpdateEvent.toJsonString());
        var eventReference = new EventReference(FILE_ENTRY_DELETE_EVENT_TOPIC, uri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventReference);
    }
}