package no.unit.nva.model.contexttypes;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.model.exceptions.InvalidSeriesException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Journal implements Periodical {

    private static final String OLD_PATH_JOURNAL = "journal";
    private static final String NEW_PATH_SERIAL_PUBLICATION = "serial-publication";
    private static final int INDEX_FROM_END_CHANNEL_TYPE_PATH_ELEMENT = 2;
    private final URI id;

    @JsonCreator
    public Journal(@JsonProperty("id") URI id) {
        validate(id);
        this.id = migratePath(id);
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

    @Deprecated
    private URI migratePath(URI id) {
        var path = UriWrapper.fromUri(id)
                       .getPath()
                       .getPathElementByIndexFromEnd(INDEX_FROM_END_CHANNEL_TYPE_PATH_ELEMENT);
        return path.equals(OLD_PATH_JOURNAL)
                   ? UriWrapper.fromUri(id).replacePathElementByIndexFromEnd(INDEX_FROM_END_CHANNEL_TYPE_PATH_ELEMENT,
                                                                             NEW_PATH_SERIAL_PUBLICATION).getUri()
                   : id;
    }
}
