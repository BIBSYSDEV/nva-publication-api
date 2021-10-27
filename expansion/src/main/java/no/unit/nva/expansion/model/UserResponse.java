package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {

    @JsonProperty("institution")
    private URI customerId;

    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }
}
