package no.unit.nva.model.contexttypes.place;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record UnconfirmedPlace(@JsonAlias(LABEL_FIELD) @JsonProperty(NAME_FIELD) String name,
                               @JsonProperty(COUNTRY_FIELD) String country) implements Place {

    private static final String COUNTRY_FIELD = "country";
    private static final String LABEL_FIELD = "label";
    private static final String NAME_FIELD = "name";

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnconfirmedPlace that)) {
            return false;
        }
        return Objects.equals(name, that.name)
               && Objects.equals(country, that.country);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String country() {
        return country;
    }
}
