package no.unit.nva.model.instancetypes.artistic.visualarts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.design.realization.Venue;
import no.unit.nva.model.pages.NullPages;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class VisualArts implements PublicationInstance<NullPages> {

    public static final String SUBTYPE_FIELD = "subtype";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String VENUES_FIELD = "venues";
    @JsonProperty(SUBTYPE_FIELD)
    private final VisualArtsSubtype subtype;
    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;
    @JsonProperty(VENUES_FIELD)
    private final Set<Venue> venues;

    public VisualArts(@JsonProperty(SUBTYPE_FIELD) VisualArtsSubtype subtype,
                      @JsonProperty(DESCRIPTION_FIELD) String description,
                      @JsonProperty(VENUES_FIELD) Set<Venue> venues) {

        this.subtype = subtype;
        this.description = description;
        this.venues = venues;
    }

    public VisualArtsSubtype getSubtype() {
        return subtype;
    }

    public String getDescription() {
        return description;
    }

    public Set<Venue> getVenues() {
        return venues;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VisualArts)) {
            return false;
        }
        VisualArts that = (VisualArts) o;
        return Objects.equals(getSubtype(), that.getSubtype())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getVenues(), that.getVenues());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getSubtype(), getDescription(), getVenues());
    }

    @Override
    public NullPages getPages() {
        return NullPages.NULL_PAGES;
    }
}
