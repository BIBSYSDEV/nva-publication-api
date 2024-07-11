package no.unit.nva.model.instancetypes.exhibition.manifestations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.model.UnconfirmedOrganization;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.time.Period;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class ExhibitionBasic implements ExhibitionProductionManifestation {
    public static final String ORGANIZATION_FIELD = "organization";
    public static final String PLACE_FIELD = "place";
    public static final String DATE_FIELD = "date";

    @JsonProperty(ORGANIZATION_FIELD)
    private final UnconfirmedOrganization organization;
    @JsonProperty(PLACE_FIELD)
    private final UnconfirmedPlace place;
    @JsonProperty(DATE_FIELD)
    private final Period date;

    @JsonCreator
    public ExhibitionBasic(@JsonProperty(ORGANIZATION_FIELD) UnconfirmedOrganization organization,
                           @JsonProperty(PLACE_FIELD) UnconfirmedPlace place,
                           @JsonProperty(DATE_FIELD) Period date) {
        this.organization = organization;
        this.place = place;
        this.date = date;
    }

    public UnconfirmedOrganization getOrganization() {
        return organization;
    }

    public UnconfirmedPlace getPlace() {
        return place;
    }

    public Period getDate() {
        return date;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExhibitionBasic)) {
            return false;
        }
        ExhibitionBasic that = (ExhibitionBasic) o;
        return Objects.equals(getOrganization(), that.getOrganization())
                && Objects.equals(getPlace(), that.getPlace())
                && Objects.equals(getDate(), that.getDate());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getOrganization(), getPlace(), getDate());
    }
}
