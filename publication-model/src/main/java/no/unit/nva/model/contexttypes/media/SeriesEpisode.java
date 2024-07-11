package no.unit.nva.model.contexttypes.media;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class SeriesEpisode {
    private static final String SERIES_NAME = "seriesName";
    private static final String SERIES_PART = "seriesPart";
    @JsonProperty(SERIES_NAME)
    @JsonAlias("series")
    private final String seriesName;
    @JsonProperty(SERIES_PART)
    private final String seriesPart;

    public SeriesEpisode(@JsonProperty(SERIES_NAME) @JsonAlias("series") String seriesName,
                         @JsonProperty(SERIES_PART) String seriesPart) {
        this.seriesName = seriesName;
        this.seriesPart = seriesPart;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public String getSeriesPart() {
        return seriesPart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SeriesEpisode)) {
            return false;
        }
        SeriesEpisode that = (SeriesEpisode) o;
        return Objects.equals(getSeriesName(), that.getSeriesName())
                && Objects.equals(getSeriesPart(), that.getSeriesPart());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSeriesName(), getSeriesPart());
    }
}
