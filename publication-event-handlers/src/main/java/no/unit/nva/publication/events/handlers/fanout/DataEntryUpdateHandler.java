package no.unit.nva.publication.events.handlers.fanout;

import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toDao;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.storage.model.DataEntry;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataEntryUpdateHandler
    extends EventHandler<DynamodbEvent.DynamodbStreamRecord, DataEntryUpdateEvent> {

    public static final String MAPPING_ERROR = "Error mapping Dynamodb Image to Publication";
    public static final DataEntry NO_VALUE = null;
    private static final Logger logger = LoggerFactory.getLogger(DataEntryUpdateHandler.class);

    @JacocoGenerated
    public DataEntryUpdateHandler() {
        super(DynamodbEvent.DynamodbStreamRecord.class);
    }

    @Override
    protected DataEntryUpdateEvent processInput(
        DynamodbEvent.DynamodbStreamRecord input,
        AwsEventBridgeEvent<DynamodbEvent.DynamodbStreamRecord> event,
        Context context) {
        String eventJson = attempt(() -> PublicationEventsConfig.objectMapper
            .writeValueAsString(event))
            .orElseThrow();
        logger.info("event:" + eventJson);

        DataEntryUpdateEvent output = new DataEntryUpdateEvent(
            input.getEventName(),
            getDao(input.getDynamodb().getOldImage()),
            getDao(input.getDynamodb().getNewImage())
        );

        String outputJson = attempt(() -> PublicationEventsConfig.objectMapper
            .writeValueAsString(output))
            .orElseThrow();
        logger.info("output" + outputJson);
        return output;
    }

    private DataEntry getDao(Map<String, AttributeValue> image) {
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
