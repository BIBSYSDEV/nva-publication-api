package no.unit.nva.publication.commons.customer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerApiRightsRetention {
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_ID = "id";

    @JsonProperty(FIELD_TYPE)
    private final String type;
    @JsonProperty(FIELD_ID)
    private final String id;
    @JsonCreator
    public CustomerApiRightsRetention(@JsonProperty(FIELD_TYPE) String type,
                                      @JsonProperty(FIELD_ID) String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
