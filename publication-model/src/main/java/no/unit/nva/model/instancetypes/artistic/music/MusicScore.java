package no.unit.nva.model.instancetypes.artistic.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.instancetypes.artistic.UnconfirmedPublisherMigrator;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class MusicScore implements MusicPerformanceManifestation, UnconfirmedPublisherMigrator {

    public static final String ENSEMBLE_FIELD = "ensemble";
    public static final String MOVEMENTS_FIELD = "movements";
    public static final String EXTENT_FIELD = "extent";
    public static final String PUBLISHER_FIELD = "publisher";
    public static final String ISMN_FIELD = "ismn";

    @JsonProperty(ENSEMBLE_FIELD)
    private final String ensemble;
    @JsonProperty(MOVEMENTS_FIELD)
    private final String movements;
    @JsonProperty(EXTENT_FIELD)
    private final String extent;
    @JsonProperty(PUBLISHER_FIELD)
    private final PublishingHouse publisher;
    @JsonProperty(ISMN_FIELD)
    private final Ismn ismn;

    public MusicScore(@JsonProperty(ENSEMBLE_FIELD) String ensemble,
                      @JsonProperty(MOVEMENTS_FIELD) String movements,
                      @JsonProperty(EXTENT_FIELD) String extent,
                      @JsonProperty(PUBLISHER_FIELD) PublishingHouse publisher,
                      @JsonProperty(ISMN_FIELD) Ismn ismn) {

        this.ensemble = ensemble;
        this.movements = movements;
        this.extent = extent;
        this.publisher = publisher;
        this.ismn = ismn;
    }

    @JsonCreator
    public static MusicScore fromJson(@JsonProperty(ENSEMBLE_FIELD) String ensemble,
                                      @JsonProperty(MOVEMENTS_FIELD) String movements,
                                      @JsonProperty(EXTENT_FIELD) String extent,
                                      @JsonProperty(PUBLISHER_FIELD) Object publisher,
                                      @JsonProperty(ISMN_FIELD) Ismn ismn) {
        var publishingHouse = publisher instanceof String
                                  ? new UnconfirmedPublisher((String) publisher)
                                  : UnconfirmedPublisherMigrator.toPublisher(publisher);
        return new MusicScore(ensemble, movements, extent, publishingHouse, ismn);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getEnsemble(), getMovements(), getExtent(), getPublisher(), getIsmn());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MusicScore)) {
            return false;
        }
        MusicScore that = (MusicScore) o;
        return Objects.equals(getEnsemble(), that.getEnsemble())
               && Objects.equals(getMovements(), that.getMovements())
               && Objects.equals(getExtent(), that.getExtent())
               && Objects.equals(getPublisher(), that.getPublisher())
               && Objects.equals(getIsmn(), that.getIsmn());
    }

    public String getEnsemble() {
        return ensemble;
    }

    public String getMovements() {
        return movements;
    }

    public String getExtent() {
        return extent;
    }

    public PublishingHouse getPublisher() {
        return publisher;
    }

    public Ismn getIsmn() {
        return ismn;
    }
}
