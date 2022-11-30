package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Series {

    private final String id;

    public Series(@JsonProperty("id") String id) {
        this.id = id;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }
}
