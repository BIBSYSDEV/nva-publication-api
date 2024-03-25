package no.unit.nva.publication.events.handlers.recovery;

import com.amazonaws.services.lambda.runtime.Context;
import java.time.Instant;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public record RecoveryEventRequest(String topic) implements JsonSerializable {

    public static final String EVENT_BUS_NAME = new Environment().readEnv("EVENT_BUS_NAME");
    public static final String TOPIC = "PublicationService.Recovery.Refresh";
    public static final String MANDATORY_UNUSED_SUBTOPIC = "DETAIL.WITH.TOPIC";

    public static PutEventsRequestEntry createNewEntry(Context context) {
        return PutEventsRequestEntry.builder()
                   .eventBusName(EVENT_BUS_NAME)
                   .detail(new RecoveryEventRequest(TOPIC).toJsonString())
                   .detailType(MANDATORY_UNUSED_SUBTOPIC)
                   .source(RecoveryBatchScanHandler.class.getName())
                   .resources(context.getInvokedFunctionArn())
                   .time(Instant.now())
                   .build();
    }
}
