package no.unit.nva.doirequest.update;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
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
