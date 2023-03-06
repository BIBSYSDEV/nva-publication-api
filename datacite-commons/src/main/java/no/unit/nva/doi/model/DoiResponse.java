package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("Doi")
public class DoiResponse {

    private final URI doi;

    @JsonCreator
    public DoiResponse(@JsonProperty("doi") URI doi) {
        this.doi = doi;
    }

    public URI getDoi() {
        return doi;
    }
}
