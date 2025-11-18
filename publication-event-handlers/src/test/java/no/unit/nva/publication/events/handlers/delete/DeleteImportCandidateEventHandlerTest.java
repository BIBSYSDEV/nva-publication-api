package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.model.testing.ImportCandidateGenerator.randomImportCandidate;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.publication.events.bodies.DeleteImportCandidateEvent;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteImportCandidateEventHandlerTest {

    public static final Context CONTEXT = null;
    private ByteArrayOutputStream output;
    private S3Client s3Client;
    private DeleteImportCandidateEventHandler handler;

    @BeforeEach
    public void init() {
        output = new ByteArrayOutputStream();
        s3Client = new FakeS3Client();
        this.handler = new DeleteImportCandidateEventHandler(s3Client);
    }

    @Test
    void shouldProduceAnExpandedDataEntryDeleteEvent() throws IOException {
        var oldImage = randomImportCandidate();
        var request = emulateEventEmittedByImportCandidateUpdateHandler(oldImage, null);
        handler.handleRequest(request, output, CONTEXT);
        var response = objectMapper.readValue(output.toString(), DeleteImportCandidateEvent.class);
        assertThat(oldImage.getIdentifier(), is(equalTo(response.getIdentifier())));
    }

    @Test
    void shouldTrowExceptionWhenBlobToDeleteIsEmpty() throws IOException {
        var request = emulateEventEmittedByImportCandidateUpdateHandler(null, null);

        assertThrows(IllegalStateException.class, () -> handler.handleRequest(request, output, CONTEXT));
    }

    private InputStream emulateEventEmittedByImportCandidateUpdateHandler(ImportCandidate oldImage,
                                                                          ImportCandidate newImage)
        throws IOException {
        var blobUri = createSampleBlob(oldImage, newImage);
        var event = new EventReference("ImportCandidates.Resource.Update", blobUri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(event);
    }

    private URI createSampleBlob(ImportCandidate oldImage, ImportCandidate newImage) throws IOException {
        var dataEntryUpdateEvent =
            new ImportCandidateDataEntryUpdate("ImportCandidates.Resource.Update", oldImage, newImage);
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        var s3Writer = new S3Driver(s3Client, EVENTS_BUCKET);
        return s3Writer.insertFile(filePath, dataEntryUpdateEvent.toJsonString());
    }
}
