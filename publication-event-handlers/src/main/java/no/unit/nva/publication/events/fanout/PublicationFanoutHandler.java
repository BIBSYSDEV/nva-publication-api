package no.unit.nva.publication.events.fanout;

import static no.unit.nva.publication.events.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static no.unit.nva.publication.events.fanout.DynamodbStreamRecordDaoMapper.toDao;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationFanoutHandler
    extends EventHandler<DynamodbEvent.DynamodbStreamRecord, DynamoEntryUpdateEvent> {

    public static final String MAPPING_ERROR = "Error mapping Dynamodb Image to Publication";
    public static final ResourceUpdate NO_VALUE = null;
    private static final Logger logger = LoggerFactory.getLogger(PublicationFanoutHandler.class);

    @JacocoGenerated
    public PublicationFanoutHandler() {
        super(DynamodbEvent.DynamodbStreamRecord.class);
    }

    @Override
    protected DynamoEntryUpdateEvent processInput(
        DynamodbEvent.DynamodbStreamRecord input,
        AwsEventBridgeEvent<DynamodbEvent.DynamodbStreamRecord> event,
        Context context) {
        String eventJson = attempt(() -> dynamoImageSerializerRemovingEmptyFields.writeValueAsString(event)).orElseThrow();
        logger.info("event:" + eventJson);
        ResourceUpdate oldDao = getDao(input.getDynamodb().getOldImage());
        ResourceUpdate newDao = getDao(input.getDynamodb().getNewImage());
        String eventType = getEventType(oldDao, newDao);
        String updateType = input.getEventName();

        DynamoEntryUpdateEvent output = new DynamoEntryUpdateEvent(
                eventType,
                updateType,
                oldDao,
                newDao
        );

        String outputJson = attempt(() -> dynamoImageSerializerRemovingEmptyFields.writeValueAsString(output)).orElseThrow();
        logger.info("output" + outputJson);
        return output;
    }

    private String getEventType(ResourceUpdate oldDao, ResourceUpdate newDao) {
        String eventType = null;
        if (oldDao instanceof Resource || newDao instanceof Resource) {
            eventType = DynamoEntryUpdateEvent.PUBLICATION_UPDATE_TYPE;
        }
        if (oldDao instanceof Message || newDao instanceof Message) {
            eventType = DynamoEntryUpdateEvent.MESSAGE_UPDATE_TYPE;
        }
        if (oldDao instanceof DoiRequest || newDao instanceof DoiRequest) {
            eventType = DynamoEntryUpdateEvent.DOI_REQUEST_UPDATE_TYPE;
        }
        return eventType;
    }

    private ResourceUpdate getDao(Map<String, AttributeValue> image) {
        if (image == null) {
            return NO_VALUE;
        }
        try {
            return toDao(image).orElse(NO_VALUE);
        } catch (Exception e) {
            logger.error(MAPPING_ERROR, e);
            throw new RuntimeException(MAPPING_ERROR, e);
        }
    }
}
