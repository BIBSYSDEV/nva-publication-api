package no.unit.nva.schemaorg.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
public class Organization {
    public static final String ID_FIELD = "@id";
    public static final String NAME_FIELD = "name";
    @JsonProperty(ID_FIELD)
    private final URI id;
    @JsonProperty(NAME_FIELD)
    private final String name;

    public Organization(@JsonProperty(ID_FIELD) URI id,
                        @JsonProperty(NAME_FIELD) String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Organization)) {
            return false;
        }
        Organization that = (Organization) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
