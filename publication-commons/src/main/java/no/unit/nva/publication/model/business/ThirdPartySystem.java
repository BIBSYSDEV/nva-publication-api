package no.unit.nva.publication.model.business;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ThirdPartySystem {
    WISE_FLOW("WISEflow"), INSPERA("Inspera"), OTHER("Other");

    private final String value;

    ThirdPartySystem(String value) {
        this.value = value;
    }

    public static ThirdPartySystem fromValue(String value) {
        return stream(values()).filter(thirdPartySystem -> thirdPartySystem.getValue().equalsIgnoreCase(value.trim()))
                   .findAny()
                   .orElse(OTHER);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
