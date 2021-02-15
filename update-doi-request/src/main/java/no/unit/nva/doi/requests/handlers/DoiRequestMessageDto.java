package no.unit.nva.doi.requests.handlers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DoiRequestMessageDto {

    private final String message;

    @JsonCreator
    public DoiRequestMessageDto(@JsonProperty("message") String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
