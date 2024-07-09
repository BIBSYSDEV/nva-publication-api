package no.unit.nva.model.instancetypes.exhibition.manifestations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.time.Instant;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ExhibitionOtherPresentation implements ExhibitionProductionManifestation {
    public static final String DESCRIPTION_FIELD = "description";
    public static final String PLACE_FIELD = "place";
    public static final String PUBLISHER_FIELD = "publisher";
    public static final String DATE_FIELD = "date";
    private static final String TYPE_DESCRIPTION_FIELD = "typeDescription";

    @JsonProperty(TYPE_DESCRIPTION_FIELD)
    private final String typeDescription;
    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;
    @JsonProperty(PLACE_FIELD)
    private final UnconfirmedPlace place;
    @JsonProperty(PUBLISHER_FIELD)
    private final PublishingHouse publisher;
    @JsonProperty(DATE_FIELD)
    private final Instant date;

    @JsonCreator
    public ExhibitionOtherPresentation(
            @JsonProperty(TYPE_DESCRIPTION_FIELD) String typeDescription,
            @JsonProperty(DESCRIPTION_FIELD) String description,
            @JsonProperty(PLACE_FIELD) UnconfirmedPlace place,
            @JsonProperty(PUBLISHER_FIELD) PublishingHouse publisher,
            @JsonProperty(DATE_FIELD) Instant date) {
        this.typeDescription = typeDescription;
        this.description = description;
        this.place = place;
        this.publisher = publisher;
        this.date = date;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public String getDescription() {
        return description;
    }

    public UnconfirmedPlace getPlace() {
        return place;
    }

    public PublishingHouse getPublisher() {
        return publisher;
    }

    public Instant getDate() {
        return date;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExhibitionOtherPresentation)) {
            return false;
        }
        ExhibitionOtherPresentation that = (ExhibitionOtherPresentation) o;
        return Objects.equals(getTypeDescription(), that.getTypeDescription())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getPlace(), that.getPlace())
                && Objects.equals(getPublisher(), that.getPublisher())
                && Objects.equals(getDate(), that.getDate());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getTypeDescription(), getDescription(), getPlace(), getPublisher(), getDate());
    }
}
