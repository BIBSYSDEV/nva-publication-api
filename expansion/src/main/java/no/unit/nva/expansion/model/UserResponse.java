package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.JsonConfig;

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

    public static UserResponse fromJson(String json) throws JsonProcessingException {
        return JsonConfig.objectMapper.readValue(json, UserResponse.class);
    }
}
