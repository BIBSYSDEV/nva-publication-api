package no.sikt.nva.brage.migration.lambda.cleanup.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InputUri {

    private String uri;

    @JsonProperty("inputUri")
    public String getUri() {
        return uri;
    }

    @JsonCreator
    public void setUri(String uri) {
        this.uri = uri;
    }
}
