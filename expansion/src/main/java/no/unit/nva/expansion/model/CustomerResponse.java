package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.ExpansionConfig;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerResponse {

    @JsonProperty("cristinId")
    private URI cristinId;

    public URI getCristinId() {
        return cristinId;
    }

    public void setCristinId(URI cristinId) {
        this.cristinId = cristinId;
    }

    public static CustomerResponse fromJson(String json) throws JsonProcessingException {
        return ExpansionConfig.objectMapper.readValue(json, CustomerResponse.class);
    }
}
