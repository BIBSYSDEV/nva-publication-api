package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteFileFromS3EventHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    public static final Logger logger = LoggerFactory.getLogger(DeleteFileFromS3EventHandler.class);
    public static final String FILE_DELETED_MESSAGE =
        "File with key {} has been deleted from S3 bucket for publication {}";
    private static final String PERSISTED_STORAGE_BUCKET_NAME = new Environment().readEnv(
        "NVA_PERSISTED_STORAGE_BUCKET_NAME");
    private final S3Client s3Client;

    @JacocoGenerated
    public DeleteFileFromS3EventHandler() {
        this(S3Driver.defaultS3Client().build());
    }

    protected DeleteFileFromS3EventHandler(S3Client s3Client) {
        super(EventReference.class);
        this.s3Client = s3Client;
    }

    @Override
    protected Void processInputPayload(EventReference eventReference,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> awsEventBridgeEvent,
                                       Context context) {

        var event = getEvent(eventReference);
        if (event.isDeleteEvent()) {
            var fileEntry = (FileEntry) event.getOldData();
            var key = fileEntry.getIdentifier().toString();
            deleteFile(key);
            logger.info(FILE_DELETED_MESSAGE, key, fileEntry.getResourceIdentifier());
        }

        return null;
    }

    private void deleteFile(String key) {
        new S3Driver(s3Client, PERSISTED_STORAGE_BUCKET_NAME).deleteFile(UnixPath.of(key));
    }

    private DataEntryUpdateEvent getEvent(EventReference eventReference) {
        var json = new S3Driver(s3Client, EVENTS_BUCKET).readEvent(eventReference.getUri());
        return DataEntryUpdateEvent.fromJson(json);
    }
}
