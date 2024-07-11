package no.unit.nva.model.instancetypes.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class OtherPresentation extends ConferenceLecture {
    @JsonCreator

    public OtherPresentation() {
        super();
    }
}
