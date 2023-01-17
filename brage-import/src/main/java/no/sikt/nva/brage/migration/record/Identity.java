package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Identity {

    private final String name;
    private final String identifier;

    public Identity(@JsonProperty("name") String name,
                    @JsonProperty("identifier") String identifier) {
        this.name = name;
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    @JacocoGenerated
    public String getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getIdentifier());
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
        Identity identity = (Identity) o;
        return Objects.equals(getName(), identity.getName()) && Objects.equals(getIdentifier(),
                                                                               identity.getIdentifier());
    }
}
