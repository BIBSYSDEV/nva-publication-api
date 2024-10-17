package no.unit.nva.publication.events.handlers.recovery;

import com.amazonaws.services.lambda.runtime.Context;
import java.time.Instant;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.Environment;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public record RecoveryEventRequest(String topic, int messagesCount) implements JsonSerializable {

    public static final String EVENT_BUS_NAME = new Environment().readEnv("EVENT_BUS_NAME");
    public static final String TOPIC = "PublicationService.Recovery.Refresh";
    public static final String MANDATORY_UNUSED_SUBTOPIC = "DETAIL.WITH.TOPIC";
    private static final int DEFAULT_MESSAGES_COUNT = 10;

    public static PutEventsRequestEntry createNewEntry(Context context) {
        return PutEventsRequestEntry.builder()
                   .eventBusName(EVENT_BUS_NAME)
                   .detail(RecoveryEventRequest.builder().build().toJsonString())
                   .detailType(MANDATORY_UNUSED_SUBTOPIC)
                   .source(RecoveryBatchScanHandler.class.getName())
                   .resources(context.getInvokedFunctionArn())
                   .time(Instant.now())
                   .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String topic = TOPIC;

        private int messagesCount = DEFAULT_MESSAGES_COUNT;

        private Builder() {
        }

        public Builder withTopic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder withMessagesCount(int messagesCount) {
            this.messagesCount = messagesCount;
            return this;
        }

        public RecoveryEventRequest build() {
            return new RecoveryEventRequest(topic, messagesCount);
        }
    }
}
