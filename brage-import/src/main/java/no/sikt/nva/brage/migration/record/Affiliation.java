package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Affiliation {

    private final String identifier;
    private final String handle;
    private final String name;

    @JsonCreator
    public Affiliation(@JsonProperty("identifier") String identifier,
                       @JsonProperty("name") String name,
                       @JsonProperty("handle") String handle) {
        this.identifier = identifier;
        this.name = name;
        this.handle = handle;
    }

    @JsonProperty("handle")
    public String getHandle() {
        return handle;
    }

    @JsonProperty("identifier")
    public String getIdentifier() {
        return identifier;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getHandle(), getName());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Affiliation that = (Affiliation) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getHandle(), that.getHandle())
               && Objects.equals(getName(), that.getName());
    }
}

