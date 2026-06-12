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
import java.util.stream.Collectors;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;
import software.amazon.awssdk.core.SdkBytes;

public class DynamoDbEventTestFactory {

  public static DynamodbEvent dynamodbEventEventWithSingleDynamoDbRecord(
      ImportCandidate oldImage, ImportCandidate newImage) {
    var event = new DynamodbEvent();
    var record = randomDynamoRecord();
    record.getDynamodb().setOldImage(toDynamoDbFormat(oldImage));
    record.getDynamodb().setNewImage(toDynamoDbFormat(newImage));
    event.setRecords(List.of(record));
    return event;
  }

  /**
   * Converts an SDK v2 DynamoDB attribute-value map (as produced by {@code Dao.toDynamoFormat()})
   * into the Lambda-events attribute-value map used to represent a DynamoDB stream record image.
   */
  public static Map<String, AttributeValue> toEventImage(
      Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> dynamoFormat) {
    return dynamoFormat.entrySet().stream()
        .collect(
            Collectors.toMap(Map.Entry::getKey, entry -> toLambdaAttributeValue(entry.getValue())));
  }

  private static AttributeValue toLambdaAttributeValue(
      software.amazon.awssdk.services.dynamodb.model.AttributeValue value) {
    var result = new AttributeValue();
    if (nonNull(value.s())) {
      result.setS(value.s());
    } else if (nonNull(value.n())) {
      result.setN(value.n());
    } else if (nonNull(value.bool())) {
      result.setBOOL(value.bool());
    } else if (nonNull(value.nul())) {
      result.setNULL(value.nul());
    } else if (value.hasM()) {
      result.setM(toEventImage(value.m()));
    } else if (value.hasL()) {
      result.setL(
          value.l().stream().map(DynamoDbEventTestFactory::toLambdaAttributeValue).toList());
    } else if (value.hasSs()) {
      result.setSS(value.ss());
    } else if (value.hasNs()) {
      result.setNS(value.ns());
    } else if (nonNull(value.b())) {
      result.setB(value.b().asByteBuffer());
    } else if (value.hasBs()) {
      result.setBS(value.bs().stream().map(SdkBytes::asByteBuffer).toList());
    }
    return result;
  }

  private static Map<String, AttributeValue> toDynamoDbFormat(ImportCandidate importCandidate) {
    return nonNull(importCandidate) ? publicationDynamoDbFormat(importCandidate) : null;
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

  private static Map<String, AttributeValue> publicationDynamoDbFormat(
      ImportCandidate importCandidate) {
    var dao =
        new ImportCandidateDao(importCandidate, importCandidate.getIdentifier()).toDynamoFormat();
    return toEventImage(dao);
  }
}
