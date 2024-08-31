package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = AdditionalIdentifier.TYPE, value = AdditionalIdentifier.class),
    @JsonSubTypes.Type(name = HandleIdentifier.TYPE, value = HandleIdentifier.class),
    @JsonSubTypes.Type(name = ScopusIdentifier.TYPE, value = ScopusIdentifier.class),
    @JsonSubTypes.Type(name = CristinIdentifier.TYPE, value = CristinIdentifier.class)
})
@Schema(oneOf = {AdditionalIdentifier.class, HandleIdentifier.class, ScopusIdentifier.class, CristinIdentifier.class})
public interface AdditionalIdentifierBase {
    @JsonProperty("value")
    String value();
    @JsonProperty("sourceName")
    @JsonAlias("source")
    String sourceName();
}
