package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import no.unit.nva.model.contexttypes.media.MediaFormat;
import no.unit.nva.model.contexttypes.media.MediaSubType;
import no.unit.nva.model.contexttypes.media.SeriesEpisode;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MediaContribution implements PublicationContext {

    private static final String MEDIUM = "medium";
    private static final String FORMAT = "format";
    private static final String DISSEMINATION_CHANNEL = "disseminationChannel";
    private static final String PART_OF = "partOf";

    @JsonProperty(MEDIUM)
    private final MediaSubType medium;

    @JsonProperty(FORMAT)
    private final MediaFormat format;

    // Since we have no clue what this is, we need to revisit
    // the use before making it required, which it apparently is.
    @JsonProperty(DISSEMINATION_CHANNEL)
    private final String disseminationChannel;

    @JsonProperty(PART_OF)
    private final SeriesEpisode partOf;


    public MediaContribution(@JsonProperty(MEDIUM) MediaSubType medium,
                             @JsonProperty(FORMAT) MediaFormat format,
                             @JsonProperty(DISSEMINATION_CHANNEL) String disseminationChannel,
                             @JsonProperty(PART_OF) SeriesEpisode partOf) {
        this.medium = medium;
        this.format = format;
        this.disseminationChannel = disseminationChannel;
        this.partOf = partOf;
    }

    public MediaSubType getMedium() {
        return medium;
    }

    public MediaFormat getFormat() {
        return format;
    }

    public String getDisseminationChannel() {
        return disseminationChannel;
    }

    public SeriesEpisode getPartOf() {
        return partOf;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaContribution)) {
            return false;
        }
        MediaContribution that = (MediaContribution) o;
        return Objects.equals(getMedium(), that.getMedium())
                && getFormat() == that.getFormat()
                && Objects.equals(getDisseminationChannel(), that.getDisseminationChannel())
                && Objects.equals(getPartOf(), that.getPartOf());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getMedium(), getFormat(), getDisseminationChannel(), getPartOf());
    }


    public static final class Builder {
        private MediaSubType medium;
        private MediaFormat format;
        // Since we have no clue what this is, we need to revisit
        // the use before making it required, which it apparently is.
        private String disseminationChannel;
        private SeriesEpisode partOf;

        public Builder() {
        }


        public Builder withMedium(MediaSubType medium) {
            this.medium = medium;
            return this;
        }

        public Builder withFormat(MediaFormat format) {
            this.format = format;
            return this;
        }

        public Builder withDisseminationChannel(String disseminationChannel) {
            this.disseminationChannel = disseminationChannel;
            return this;
        }

        public Builder withPartOf(SeriesEpisode partOf) {
            this.partOf = partOf;
            return this;
        }

        public MediaContribution build() {
            return new MediaContribution(medium, format, disseminationChannel, partOf);
        }
    }
}
