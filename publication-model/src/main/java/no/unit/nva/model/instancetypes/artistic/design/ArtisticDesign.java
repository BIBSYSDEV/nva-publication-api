package no.unit.nva.model.instancetypes.artistic.design;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.artistic.design.realization.Venue;
import no.unit.nva.model.pages.NullPages;
import nva.commons.core.JacocoGenerated;

import java.util.List;
import java.util.Objects;

import static no.unit.nva.model.util.SerializationUtils.nullListAsEmpty;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ArtisticDesign implements PublicationInstance<NullPages> {

    public static final String SUBTYPE_FIELD = "subtype";
    public static final String DESCRIPTION_FIELD = "description";
    public static final String VENUES_FIELD = "venues";

    @JsonProperty(SUBTYPE_FIELD)
    private final ArtisticDesignSubtype subtype;
    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;
    @JsonProperty(VENUES_FIELD)
    private final List<Venue> venues;


    public ArtisticDesign(@JsonProperty(SUBTYPE_FIELD) ArtisticDesignSubtype subtype,
                          @JsonProperty(DESCRIPTION_FIELD) String description,
                          @JsonProperty(VENUES_FIELD) List<Venue> venues) {
        this.subtype = subtype;
        this.description = description;
        this.venues = nullListAsEmpty(venues);
    }

    public ArtisticDesignSubtype getSubtype() {
        return subtype;
    }

    public String getDescription() {
        return description;
    }

    @JsonGetter
    @Override
    public NullPages getPages() {
        return NullPages.NULL_PAGES;
    }

    public List<Venue> getVenues() {
        return venues;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArtisticDesign)) {
            return false;
        }
        ArtisticDesign that = (ArtisticDesign) o;
        return Objects.equals(getSubtype(), that.getSubtype())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getVenues(), that.getVenues());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getSubtype(), getDescription(), getVenues());
    }
}
