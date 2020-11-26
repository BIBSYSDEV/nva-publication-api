package no.unit.nva.publication.doi.update.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.json.JsonSerializable;

public class DoiUpdateHolder implements JsonSerializable {

    public static final String DEFAULT_TYPE = "doi.updateDoiStatus";
    protected String type;
    protected DoiUpdateDto item;

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
