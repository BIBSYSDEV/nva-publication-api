package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("Doi")
public class DoiResponse {

    private final String doi;

    @JsonCreator
    public DoiResponse(@JsonProperty("doi") String doi) {
        this.doi = doi;
    }

    public String getDoi() {
        return doi;
    }
}
