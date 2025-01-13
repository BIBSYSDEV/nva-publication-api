package no.unit.nva.model.contexttypes;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.model.exceptions.InvalidSeriesException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Series implements BookSeries {

    private final URI id;

    @JsonCreator
    public Series(@JsonProperty("id") URI id) {
        validate(id);
        this.id = id;
    }

    public URI getId() {
        return id;
    }

    @Override
    public boolean isConfirmed() {
        return true;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Series)) {
            return false;
        }
        Series series = (Series) o;
        return Objects.equals(getId(), series.getId());
    }

    private static void validate(URI id) {
        var stringOfUri = id.toString();
        if (isNull(stringOfUri) || stringOfUri.isBlank()) {
            throw new InvalidSeriesException(stringOfUri);
        }
    }
}
