package no.unit.nva.model.contexttypes.place;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UnconfirmedPlace implements Place {
    public static final String COUNTRY = "country";
    public static final String LABEL = "label";

    @JsonProperty(LABEL)
    private final String label;
    @JsonProperty(COUNTRY)
    private final String country;

    public UnconfirmedPlace(@JsonProperty(LABEL) String label, @JsonProperty(COUNTRY) String country) {
        this.label = label;
        this.country = country;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnconfirmedPlace)) {
            return false;
        }
        UnconfirmedPlace that = (UnconfirmedPlace) o;
        return Objects.equals(label, that.label)
                && Objects.equals(country, that.country);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(label, country);
    }

    public String getLabel() {
        return label;
    }

    public String getCountry() {
        return country;
    }
}
