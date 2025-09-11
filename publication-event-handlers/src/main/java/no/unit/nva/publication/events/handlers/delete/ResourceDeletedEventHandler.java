package no.unit.nva.publication.events.handlers.delete;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ResourceDeletedEventHandler extends DestinationsEventBridgeEventHandler<EventReference, Void> {

    private static final String RESOURCE_STORAGE_BUCKET_NAME_ENV_KEY = "RESOURCE_STORAGE_BUCKET_NAME";

    private final S3Driver eventsS3Driver;
    private final S3Driver resourceStorageS3Driver;
    private final ResourceService resourceService;
    private final Logger logger = LoggerFactory.getLogger(ResourceDeletedEventHandler.class);

    @JacocoGenerated
    public ResourceDeletedEventHandler() {
        this(new Environment(), S3Driver.defaultS3Client().build(), ResourceService.defaultService());
    }

    public ResourceDeletedEventHandler(Environment environment, S3Client s3Client, ResourceService resourceService) {
        super(EventReference.class);
        this.eventsS3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
        this.resourceStorageS3Driver = new S3Driver(s3Client,
                                                    environment.readEnv(RESOURCE_STORAGE_BUCKET_NAME_ENV_KEY));
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInputPayload(EventReference input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                       Context context) {
        var eventContent = eventsS3Driver.readEvent(input.getUri());
        logger.info("Received event: {}", eventContent);
        var entryUpdate = DataEntryUpdateEvent.fromJson(eventContent);
        var deletedResource = (Resource) entryUpdate.getOldData();

        resourceService.deleteAllResourceAssociatedEntries(deletedResource.getCustomerId(),
                                                           deletedResource.getIdentifier());
        deletedResource.getFiles().forEach(this::deleteFromS3IfStillPresent);
        return null;
    }

    private void deleteFromS3IfStillPresent(File file) {
        resourceStorageS3Driver.deleteFile(UnixPath.of(file.getIdentifier().toString()));
    }
}
