package no.unit.nva.model.contexttypes.place;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record UnconfirmedPlace(@JsonAlias(LABEL_FIELD) String name, String country)
        implements Place {
    private static final String LABEL_FIELD = "label";
}
