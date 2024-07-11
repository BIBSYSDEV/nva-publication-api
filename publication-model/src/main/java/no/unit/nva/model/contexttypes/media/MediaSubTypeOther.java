package no.unit.nva.model.contexttypes.media;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MediaSubTypeOther extends MediaSubType {
    public static final String DESCRIPTION = "description";
    @JsonProperty(DESCRIPTION)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;

    protected MediaSubTypeOther(@JsonProperty(TYPE) MediaSubTypeEnum type,
                                @JsonProperty(DESCRIPTION) String description) {
        super(type);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
