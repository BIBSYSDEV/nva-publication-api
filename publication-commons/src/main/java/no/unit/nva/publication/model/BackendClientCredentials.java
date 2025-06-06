package no.unit.nva.publication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

public class BackendClientCredentials {

    @JsonProperty("backendClientId")
    public String id;

    @JsonProperty("backendClientSecret")
    public String secret;

    @JacocoGenerated
    public BackendClientCredentials() {
    }

    public BackendClientCredentials(String id, String secret) {
        this.id = id;
        this.secret = secret;
    }

    public String getId() {
        return id;
    }

    public String getSecret() {
        return secret;
    }

    @Override
    public String toString() {
        return """
               {
                 "backendClientId": "%s",
                 "backendClientSecret": "%s"
               }
               """.formatted(id, secret);
    }
}
