package no.unit.nva.publication.events.fanout;

import static no.unit.nva.publication.events.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static no.unit.nva.publication.events.fanout.DynamodbStreamRecordPublicationMapper.toPublication;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.DynamoEntryUpdateEvent;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationFanoutHandler
    extends EventHandler<DynamodbEvent.DynamodbStreamRecord, DynamoEntryUpdateEvent> {

    public static final String MAPPING_ERROR = "Error mapping Dynamodb Image to Publication";
    public static final Publication NO_VALUE = null;
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
        String eventJson = attempt(() -> dynamoImageSerializerRemovingEmptyFields.writeValueAsString(event))
                .orElseThrow();
        logger.info("event:" + eventJson);
        Optional<Publication> oldPublication = getPublication(input.getDynamodb().getOldImage());
        Optional<Publication> newPublication = getPublication(input.getDynamodb().getNewImage());
        String updateType = input.getEventName();

        DynamoEntryUpdateEvent output = new DynamoEntryUpdateEvent(
            DynamoEntryUpdateEvent.PUBLICATION_UPDATE_TYPE,
            updateType,
            oldPublication.orElse(NO_VALUE),
            newPublication.orElse(NO_VALUE)
        );

        String outputJson = attempt(() -> dynamoImageSerializerRemovingEmptyFields.writeValueAsString(output))
                .orElseThrow();
        logger.info("output" + outputJson);
        return output;
    }

    private Optional<Publication> getPublication(Map<String, AttributeValue> image) {
        if (image == null) {
            return Optional.empty();
        }
        try {
            return toPublication(image);
        } catch (Exception e) {
            logger.error(MAPPING_ERROR, e);
            throw new RuntimeException(MAPPING_ERROR, e);
        }
    }
}
