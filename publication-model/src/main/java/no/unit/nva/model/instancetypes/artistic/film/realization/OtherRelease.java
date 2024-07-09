package no.unit.nva.model.instancetypes.artistic.film.realization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.instancetypes.artistic.UnconfirmedPublisherMigrator;
import no.unit.nva.model.time.Instant;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class OtherRelease implements MovingPictureOutput, UnconfirmedPublisherMigrator {
    public static final String DESCRIPTION_FIELD = "description";
    public static final String PLACE_FIELD = "place";
    public static final String PUBLISHER_FIELD = "publisher";
    public static final String DATE_FIELD = "date";

    @JsonProperty(DESCRIPTION_FIELD)
    private final String description;
    @JsonProperty(PLACE_FIELD)
    private final UnconfirmedPlace place;
    @JsonProperty(PUBLISHER_FIELD)
    private final PublishingHouse publisher;
    @JsonProperty(DATE_FIELD)
    private final Instant date;
    @JsonProperty(SEQUENCE_FIELD)
    private final int sequence;

    @Deprecated
    @JsonCreator
    public static OtherRelease fromJson(@JsonProperty(DESCRIPTION_FIELD) String description,
                           @JsonProperty(PLACE_FIELD) UnconfirmedPlace place,
                           @JsonProperty(PUBLISHER_FIELD) Object publisher,
                           @JsonProperty(DATE_FIELD) Instant date,
                           @JsonProperty(SEQUENCE_FIELD) int sequence) {
        var publishingHouse = publisher instanceof String
                ? new UnconfirmedPublisher((String) publisher)
                : UnconfirmedPublisherMigrator.toPublisher(publisher);
        return new OtherRelease(description, place, publishingHouse, date, sequence);
    }

    public OtherRelease(@JsonProperty(DESCRIPTION_FIELD) String description,
                        @JsonProperty(PLACE_FIELD) UnconfirmedPlace place,
                        @JsonProperty(PUBLISHER_FIELD) PublishingHouse publisher,
                        @JsonProperty(DATE_FIELD) Instant date,
                        @JsonProperty(SEQUENCE_FIELD) int sequence) {
        this.description = description;
        this.place = place;
        this.publisher = publisher;
        this.date = date;
        this.sequence = sequence;
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
    public int getSequence() {
        return sequence;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof OtherRelease)) {
            return false;
        }
        OtherRelease that = (OtherRelease) o;
        return getSequence() == that.getSequence()
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getPlace(), that.getPlace())
                && Objects.equals(getPublisher(), that.getPublisher())
                && Objects.equals(getDate(), that.getDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getPlace(), getPublisher(), getDate(), getSequence());
    }
}
