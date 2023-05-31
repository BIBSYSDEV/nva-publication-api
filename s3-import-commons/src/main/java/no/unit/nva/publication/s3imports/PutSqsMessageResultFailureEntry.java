package no.unit.nva.publication.s3imports;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.events.models.EventReference;
import nva.commons.core.JacocoGenerated;

public class PutSqsMessageResultFailureEntry implements JsonSerializable {

    private final EventReference event;
    private final String cause;

    public PutSqsMessageResultFailureEntry(@JsonProperty("event") EventReference event,
                                           @JsonProperty("cause") String cause) {
        this.event = event;
        this.cause = cause;
    }

    public EventReference getEvent() {
        return event;
    }

    @JacocoGenerated
    public String getCause() {
        return cause;
    }
}
