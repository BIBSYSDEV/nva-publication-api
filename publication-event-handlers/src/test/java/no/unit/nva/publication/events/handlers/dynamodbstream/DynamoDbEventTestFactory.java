package no.unit.nva.publication.events.handlers.dynamodbstream;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.AWS_REGION;
import static no.unit.nva.publication.testing.AttributeValueConvert.toLambdaAttributeValueMap;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import java.util.List;
import java.util.Map;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;

public class DynamoDbEventTestFactory {

    public static DynamodbEvent dynamodbEventEventWithSingleDynamoDbRecord(ImportCandidate oldImage,
                                                                           ImportCandidate newImage) {
        var event = new DynamodbEvent();
        var record = randomDynamoRecord();
        record.getDynamodb().setOldImage(toDynamoDbFormat(oldImage));
        record.getDynamodb().setNewImage(toDynamoDbFormat(newImage));
        event.setRecords(List.of(record));
        return event;
    }

    private static Map<String, AttributeValue> toDynamoDbFormat(ImportCandidate importCandidate) {
        if (!nonNull(importCandidate)) {
            return null;
        }
        var sdkMap = new ImportCandidateDao(importCandidate, importCandidate.getIdentifier()).toDynamoFormat();
        return toLambdaAttributeValueMap(sdkMap);
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
}
