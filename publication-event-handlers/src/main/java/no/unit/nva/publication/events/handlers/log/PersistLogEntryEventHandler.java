package no.unit.nva.publication.events.handlers.log;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toEntity;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;

public class PersistLogEntryEventHandler extends EventHandler<EventReference, Void> {

    private final S3Client s3Client;
    private final ResourceService resourceService;

    @JacocoGenerated
    protected PersistLogEntryEventHandler() {
        this(S3Driver.defaultS3Client().build(), ResourceService.defaultService());
    }

    protected PersistLogEntryEventHandler(S3Client s3Client, ResourceService resourceService) {
        super(EventReference.class);
        this.s3Client = s3Client;
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInput(EventReference eventReference, AwsEventBridgeEvent<EventReference> awsEventBridgeEvent,
                                Context context) {
        return readNewImageResourceIdentifier(eventReference).map(this::handleNewImage).orElse(null);
    }

    private static DynamodbStreamRecord parseDynamoDbStreamRecord(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, DynamodbStreamRecord.class)).orElseThrow();
    }

    private Void handleNewImage(SortableIdentifier resourceIdentifier) {
        var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService);
        if (resource.hasResourceEvent()) {
            resource.getResourceEvent().toLogEntry(resourceIdentifier).persist(resourceService);
            resource.clearResourceEvent(resourceService);
        }
        return null;
    }

    private Optional<SortableIdentifier> readNewImageResourceIdentifier(EventReference input) {
        return attempt(() -> fetchDynamoDbStreamRecord(input)).map(
                PersistLogEntryEventHandler::parseDynamoDbStreamRecord)
                   .map(this::convertToDataEntryUpdateEvent)
                   .map(DataEntryUpdateEvent::getNewData)
                   .map(Entity::getIdentifier)
                   .toOptional();
    }

    private String fetchDynamoDbStreamRecord(EventReference input) {
        return new S3Driver(s3Client, EVENTS_BUCKET).readEvent(input.getUri());
    }

    private Entity getEntity(Map<String, AttributeValue> image) {
        return attempt(() -> toEntity(image)).orElse(failure -> Optional.<Entity>empty()).orElse(null);
    }

    private DataEntryUpdateEvent convertToDataEntryUpdateEvent(DynamodbStreamRecord dynamoDbRecord) {
        return new DataEntryUpdateEvent(dynamoDbRecord.getEventName(),
                                        getEntity(dynamoDbRecord.getDynamodb().getOldImage()),
                                        getEntity(dynamoDbRecord.getDynamodb().getNewImage()));
    }
}
