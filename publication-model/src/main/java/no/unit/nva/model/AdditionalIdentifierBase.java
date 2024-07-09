package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = AdditionalIdentifier.TYPE, value = AdditionalIdentifier.class),
    @JsonSubTypes.Type(name = HandleIdentifier.TYPE, value = HandleIdentifier.class),
    @JsonSubTypes.Type(name = ScopusIdentifier.TYPE, value = ScopusIdentifier.class),
    @JsonSubTypes.Type(name = CristinIdentifier.TYPE, value = CristinIdentifier.class)
})
public interface AdditionalIdentifierBase {
    @JsonProperty("value")
    String value();
    @JsonProperty("sourceName")
    @JsonAlias("source")
    String sourceName();
}
