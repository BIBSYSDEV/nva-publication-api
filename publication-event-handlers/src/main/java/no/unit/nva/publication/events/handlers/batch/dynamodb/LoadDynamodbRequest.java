package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.nonNull;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.storage.KeyField;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public record LoadDynamodbRequest(String jobType,
                                  Map<String, AttributeValue> startMarker,
                                  List<KeyField> types,
                                  Integer segment,
                                  Integer totalSegments,
                                  BatchFilter filter) implements JsonSerializable {

    public boolean isSegmentedScan() {
        return nonNull(segment) && nonNull(totalSegments);
    }

    public LoadDynamodbRequest withStartMarker(Map<String, AttributeValue> newStartMarker) {
        return new LoadDynamodbRequest(this.jobType, newStartMarker, this.types, this.segment, this.totalSegments,
                                       this.filter);
    }

    public PutEventsRequestEntry createNewEventEntry(String eventBusName,
                                                     String detailType,
                                                     String invokedFunctionArn) {
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