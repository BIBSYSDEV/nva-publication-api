package no.unit.nva.model.instancetypes.artistic.film.realization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.instancetypes.artistic.UnconfirmedPublisherMigrator;
import no.unit.nva.model.time.Instant;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Broadcast implements MovingPictureOutput, UnconfirmedPublisherMigrator {

    public static final String PUBLISHER_FIELD = "publisher";
    public static final String DATE_FIELD = "date";

    @JsonProperty(PUBLISHER_FIELD)
    private final PublishingHouse publisher;
    @JsonProperty(DATE_FIELD)
    private final Instant date;
    @JsonProperty(SEQUENCE_FIELD)
    private final int sequence;

    @Deprecated
    @JsonCreator
    public static Broadcast fromJson(@JsonProperty(PUBLISHER_FIELD) Object publisher,
                           @JsonProperty(DATE_FIELD) Instant date,
                           @JsonProperty(SEQUENCE_FIELD) int sequence) {
        if (publisher instanceof String) {
            return new Broadcast(new UnconfirmedPublisher((String) publisher), date, sequence);
        }
        return new Broadcast(UnconfirmedPublisherMigrator.toPublisher(publisher), date, sequence);
    }

    public Broadcast(@JsonProperty(PUBLISHER_FIELD) PublishingHouse publisher,
                     @JsonProperty(DATE_FIELD) Instant date,
                     @JsonProperty(SEQUENCE_FIELD) int sequence) {
        this.publisher = publisher;
        this.date = date;
        this.sequence = sequence;
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
        if (!(o instanceof Broadcast)) {
            return false;
        }
        Broadcast broadcast = (Broadcast) o;
        return getSequence() == broadcast.getSequence()
               && Objects.equals(getPublisher(), broadcast.getPublisher())
               && Objects.equals(getDate(), broadcast.getDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPublisher(), getDate(), getSequence());
    }
}
