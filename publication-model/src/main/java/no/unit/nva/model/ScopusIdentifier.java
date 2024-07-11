package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ScopusIdentifier(@JsonIgnore SourceName source, String value) implements AdditionalIdentifierBase {

    static final String TYPE = "ScopusIdentifier";

    @JsonCreator
    private ScopusIdentifier(@JsonProperty("sourceName") String sourceName, @JsonProperty("value") String value) {
        this(new SourceName(sourceName), value);
    }

    @Override
    public String sourceName() {
        return source.toString();
    }
}
