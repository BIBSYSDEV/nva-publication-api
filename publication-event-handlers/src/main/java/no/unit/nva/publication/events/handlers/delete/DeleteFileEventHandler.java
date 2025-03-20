package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteFileEventHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DeleteFileEventHandler.class);
    private static final String FILE_DELETED_MESSAGE = "File with key {} has been deleted from S3 bucket for " +
                                                       "publication {}";
    private static final String PERSISTED_STORAGE_BUCKET_NAME = new Environment().readEnv(
        "NVA_PERSISTED_STORAGE_BUCKET_NAME");
    private final S3Client s3Client;
    private final ResourceService resourceService;

    @JacocoGenerated
    public DeleteFileEventHandler() {
        this(S3Driver.defaultS3Client().build(), ResourceService.defaultService());
    }

    protected DeleteFileEventHandler(S3Client s3Client, ResourceService resourceService) {
        super(EventReference.class);
        this.s3Client = s3Client;
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInputPayload(EventReference eventReference,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> awsEventBridgeEvent,
                                       Context context) {

        var fileEntry = (FileEntry) getEvent(eventReference).getNewData();
        var key = fileEntry.getIdentifier().toString();
        deleteFileFromS3(key);
        FileEntry.queryObject(fileEntry.getFile().getIdentifier(), fileEntry.getResourceIdentifier())
            .delete(resourceService);
        logger.info(FILE_DELETED_MESSAGE, key, fileEntry.getResourceIdentifier());

        return null;
    }

    private void deleteFileFromS3(String key) {
        new S3Driver(s3Client, PERSISTED_STORAGE_BUCKET_NAME).deleteFile(UnixPath.of(key));
    }

    private DataEntryUpdateEvent getEvent(EventReference eventReference) {
        var json = new S3Driver(s3Client, EVENTS_BUCKET).readEvent(eventReference.getUri());
        return DataEntryUpdateEvent.fromJson(json);
    }
}
