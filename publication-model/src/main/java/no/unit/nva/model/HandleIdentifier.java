package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public record HandleIdentifier(@JsonIgnore SourceName source, @JsonIgnore URI uri)
    implements AdditionalIdentifierBase {

    static final String TYPE = "HandleIdentifier";

    @JsonCreator
    private HandleIdentifier(@JsonProperty("sourceName") String sourceName, @JsonProperty("value") String value) {
        this(new SourceName(sourceName), URI.create(value));
    }

    @Override
    public String sourceName() {
        return source.toString();
    }

    @Override
    public String value() {
        return uri.toString();
    }
}
