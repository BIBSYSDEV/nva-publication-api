package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import java.net.URI;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class MediaContributionPeriodical extends Journal {

    public static final String ID_FIELD = "id";

    @JsonCreator
    public MediaContributionPeriodical(@JsonProperty(ID_FIELD) URI id) {
        super(id);
    }
}
