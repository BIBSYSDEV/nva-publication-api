package no.unit.nva.publication.events.handlers.dynamodbstream;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.AWS_REGION;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.databind.JavaType;
import java.util.List;
import java.util.Map;
import no.unit.nva.publication.model.business.Entity;

public class DynamoDbEventTestFactory {

    public static DynamodbEvent dynamodbEventEventWithSingleDynamoDbRecord(Entity oldImage, Entity newImage) {
        var event = new DynamodbEvent();
        var record = randomDynamoRecord();
        record.getDynamodb().setOldImage(toDynamoDbFormat(oldImage));
        record.getDynamodb().setNewImage(toDynamoDbFormat(newImage));
        event.setRecords(List.of(record));
        return event;
    }

    private static Map<String, AttributeValue> toDynamoDbFormat(Entity publication) {
        return nonNull(publication) ? publicationDynamoDbFormat(publication) : null;
    }

    private static DynamodbEvent.DynamodbStreamRecord randomDynamoRecord() {
        var streamRecord = new DynamodbStreamRecord();
        streamRecord.setEventName(randomElement(OperationType.values()));
        streamRecord.setEventID(randomString());
        streamRecord.setAwsRegion(AWS_REGION);
        var dynamodb = new StreamRecord();
        streamRecord.setDynamodb(dynamodb);
        streamRecord.setEventSource(randomString());
        streamRecord.setEventVersion(randomString());
        return streamRecord;
    }

    private static Map<String, AttributeValue> publicationDynamoDbFormat(Entity publication) {
        var dao = publication.toDao().toDynamoFormat();
        var string = attempt(() -> dtoObjectMapper.writeValueAsString(dao)).orElseThrow();
        return (Map<String, AttributeValue>) attempt(
            () -> dtoObjectMapper.readValue(string, dynamoMapStructureAsJacksonType())).orElseThrow();
    }

    private static JavaType dynamoMapStructureAsJacksonType() {
        return dtoObjectMapper.getTypeFactory().constructParametricType(Map.class, String.class, AttributeValue.class);
    }
}
