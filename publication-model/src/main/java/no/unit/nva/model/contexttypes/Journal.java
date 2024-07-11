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
public class Journal implements Periodical {

    private final URI id;

    @JsonCreator
    public Journal(@JsonProperty("id") URI id) {
        validate(id);
        this.id = id;
    }

    public URI getId() {
        return id;
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
        if (!(o instanceof Journal)) {
            return false;
        }
        Journal journal = (Journal) o;
        return Objects.equals(getId(), journal.getId());
    }

    private static void validate(URI id) {
        var stringOfUri = id.toString();
        if (isNull(stringOfUri) || stringOfUri.isBlank()) {
            throw new InvalidSeriesException(stringOfUri);
        }
    }
}
