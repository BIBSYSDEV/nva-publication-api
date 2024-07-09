package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class LiteraryArtsPerformance implements LiteraryArtsManifestation {
    public static final String SUBTYPE_FIELD = "subtype";
    public static final String PLACE_FIELD = "place";
    public static final String PUBLICATION_DATE_FIELD = "publicationDate";
    @JsonProperty(SUBTYPE_FIELD) private final LiteraryArtsPerformanceSubtype subtype;
    @JsonProperty(PLACE_FIELD) private final UnconfirmedPlace place;
    @JsonProperty(PUBLICATION_DATE_FIELD) private final PublicationDate publicationDate;

    @JsonCreator
    public LiteraryArtsPerformance(@JsonProperty(SUBTYPE_FIELD) LiteraryArtsPerformanceSubtype subtype,
                                   @JsonProperty(PLACE_FIELD) UnconfirmedPlace place,
                                   @JsonProperty(PUBLICATION_DATE_FIELD) PublicationDate publicationDate) {

        this.subtype = subtype;
        this.place = place;
        this.publicationDate = publicationDate;
    }

    @Override
    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public LiteraryArtsPerformanceSubtype getSubtype() {
        return subtype;
    }

    public UnconfirmedPlace getPlace() {
        return place;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArtsPerformance)) {
            return false;
        }
        LiteraryArtsPerformance that = (LiteraryArtsPerformance) o;
        return Objects.equals(getSubtype(), that.getSubtype())
                && Objects.equals(getPlace(), that.getPlace())
                && Objects.equals(getPublicationDate(), that.getPublicationDate());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getSubtype(), getPlace(), getPublicationDate());
    }
}
