package no.unit.nva.model.contexttypes;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Publisher implements PublishingHouse {

    private final URI id;

    @JsonCreator
    public Publisher(@JsonProperty("id") URI id) {
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
        if (!(o instanceof Publisher)) {
            return false;
        }
        Publisher publisher = (Publisher) o;
        return Objects.equals(getId(), publisher.getId());
    }

    @Override
    public boolean isValid() {
        return nonNull(id);
    }

    @JsonIgnore
    public UUID getIdentifier() {
        var identifierFromUri = UriWrapper.fromUri(id)
            .replacePathElementByIndexFromEnd(0, StringUtils.EMPTY_STRING)
            .getLastPathElement();
        return UUID.fromString(identifierFromUri);
    }
}
