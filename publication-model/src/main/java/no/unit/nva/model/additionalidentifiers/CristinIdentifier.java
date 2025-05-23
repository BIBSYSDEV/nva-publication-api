package no.unit.nva.model.additionalidentifiers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CristinIdentifier(@JsonIgnore SourceName source, String value) implements AdditionalIdentifierBase {

    static final String TYPE = "CristinIdentifier";

    @JsonCreator
    private CristinIdentifier(@JsonProperty("sourceName") String sourceName, @JsonProperty("value") String value) {
        this(new SourceName(sourceName), value);
    }

    public static AdditionalIdentifierBase fromCounter(int counter) {
        return new CristinIdentifier(SourceName.nva(), String.valueOf(counter));
    }

    @Override
    public String sourceName() {
        return source.toString();
    }
}
