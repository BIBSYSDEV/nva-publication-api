package no.unit.nva.publication.model.business;

import static java.util.Arrays.stream;
import com.fasterxml.jackson.annotation.JsonValue;
import no.unit.nva.model.ImportSource.Source;

public enum ThirdPartySystem {
    WISE_FLOW("WISEflow"), INSPERA("Inspera"), THESIS_PORTAL("Avhandlingsportalen"), OTHER("Other");

    private final String value;

    ThirdPartySystem(String value) {
        this.value = value;
    }

    public static ThirdPartySystem fromValue(String value) {
        return stream(values()).filter(thirdPartySystem -> thirdPartySystem.getValue().equalsIgnoreCase(value.trim()))
                   .findAny()
                   .orElse(OTHER);
    }

    public Source toSource() {
        return switch (this) {
            case INSPERA -> Source.INSPERA;
            case WISE_FLOW -> Source.WISE_FLOW;
            case THESIS_PORTAL -> Source.THESIS_PORTAL;
            case OTHER -> Source.OTHER;
        };
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
