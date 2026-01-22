package no.unit.nva.publication.events.handlers.dynamodbstream;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.AWS_REGION;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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

    private static Map<String, AttributeValue> toLambdaAttributeValueMap(
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> sdkMap) {
        return sdkMap.entrySet().stream()
                   .collect(Collectors.toMap(Entry::getKey, e -> toLambdaAttributeValue(e.getValue())));
    }

    private static AttributeValue toLambdaAttributeValue(
            software.amazon.awssdk.services.dynamodb.model.AttributeValue value) {
        return switch (value.type()) {
            case S -> new AttributeValue().withS(value.s());
            case SS -> new AttributeValue().withSS(value.ss());
            case N -> new AttributeValue().withN(value.n());
            case NS -> new AttributeValue().withNS(value.ns());
            case B -> new AttributeValue().withB(value.b().asByteBuffer());
            case BS -> new AttributeValue().withBS(value.bs().stream()
                           .map(b -> b.asByteBuffer()).collect(Collectors.toList()));
            case M -> new AttributeValue().withM(toLambdaAttributeValueMap(value.m()));
            case L -> new AttributeValue().withL(value.l().stream()
                          .map(DynamoDbEventTestFactory::toLambdaAttributeValue)
                          .collect(Collectors.toList()));
            case NUL -> new AttributeValue().withNULL(value.nul());
            case BOOL -> new AttributeValue().withBOOL(value.bool());
            default -> throw new IllegalArgumentException("Unknown AttributeValue type: " + value.type());
        };
    }
}
