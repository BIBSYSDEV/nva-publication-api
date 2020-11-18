package no.unit.nva.publication.doi.update.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DoiUpdateHolder implements JsonSerializable {

    public String type;
    public DoiUpdateDto item;

    @JsonCreator
    public DoiUpdateHolder(@JsonProperty("type") String type, @JsonProperty("item") DoiUpdateDto item) {
        this.type = type;
        this.item = item;
    }

    public String getType() {
        return type;
    }

    public boolean hasItem() {
        return item != null;
    }

    public DoiUpdateDto getItem() {
        return item;
    }
}
