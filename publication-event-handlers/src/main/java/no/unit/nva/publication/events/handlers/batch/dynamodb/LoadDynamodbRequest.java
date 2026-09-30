package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.storage.KeyField;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public record LoadDynamodbRequest(
    String jobType,
    Map<String, String> startMarker,
    List<KeyField> types,
    Integer segment,
    Integer totalSegments,
    BatchFilter filter,
    Integer itemsProcessed)
    implements JsonSerializable {

  public LoadDynamodbRequest(
      String jobType,
      Map<String, String> startMarker,
      List<KeyField> types,
      Integer segment,
      Integer totalSegments,
      BatchFilter filter) {
    this(jobType, startMarker, types, segment, totalSegments, filter, null);
  }

  public boolean isSegmentedScan() {
    return nonNull(segment) && nonNull(totalSegments);
  }

  public int currentItemsProcessed() {
    return isNull(itemsProcessed) ? 0 : itemsProcessed;
  }

  public Map<String, AttributeValue> toDynamodbStartMarker() {
    Map<String, AttributeValue> dynamodbStartMarker = null;
    if (nonNull(startMarker)) {
      dynamodbStartMarker =
          startMarker.entrySet().stream()
              .collect(
                  toMap(
                      Map.Entry::getKey,
                      entry -> AttributeValue.builder().s(entry.getValue()).build()));
    }
    return dynamodbStartMarker;
  }

  public LoadDynamodbRequest nextPageRequest(
      Map<String, AttributeValue> lastEvaluatedKey, int scannedItemCount) {
    var jsonSafeStartMarker =
        lastEvaluatedKey.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().s()));
    return new LoadDynamodbRequest(
        jobType,
        jsonSafeStartMarker,
        types,
        segment,
        totalSegments,
        filter,
        currentItemsProcessed() + scannedItemCount);
  }

  public PutEventsRequestEntry createNewEventEntry(
      String eventBusName, String detailType, String invokedFunctionArn) {
    return PutEventsRequestEntry.builder()
        .eventBusName(eventBusName)
        .detail(this.toJsonString())
        .detailType(detailType)
        .resources(invokedFunctionArn)
        .time(Instant.now())
        .source(invokedFunctionArn)
        .build();
  }
}
