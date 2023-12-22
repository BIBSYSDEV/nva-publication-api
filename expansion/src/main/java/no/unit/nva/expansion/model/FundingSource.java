package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import no.unit.nva.commons.json.JsonSerializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class FundingSource implements JsonSerializable {

    private final URI id;

    @JsonCreator
    public FundingSource(@JsonProperty("id") URI id) {
        this.id = id;
    }

    @JsonProperty("id")
    public URI getId() {
        return id;
    }

    @JsonValue
    @Override
    public String toString() {
        return id.toString();
    }
}
