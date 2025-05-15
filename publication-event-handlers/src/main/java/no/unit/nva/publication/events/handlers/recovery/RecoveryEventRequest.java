package no.unit.nva.publication.events.handlers.recovery;

import static java.util.Objects.isNull;
import no.unit.nva.commons.json.JsonSerializable;

public record RecoveryEventRequest(Integer maximumNumberOfMessages) implements JsonSerializable {

    private static final int DEFAULT_MESSAGES_COUNT = 10;

    public RecoveryEventRequest(Integer maximumNumberOfMessages) {
        this.maximumNumberOfMessages =
            isNull(maximumNumberOfMessages) ? DEFAULT_MESSAGES_COUNT : maximumNumberOfMessages;
    }
}
