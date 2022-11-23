package no.sikt.nva.brage.migration.model.entitydescription;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Identity {

    private static final String IDENTITY_TYPE = "Identity";
    private final String type;
    private String name;

    @JacocoGenerated
    public Identity() {
        this.type = IDENTITY_TYPE;
    }

    @JacocoGenerated
    public Identity(String name) {
        this.name = name;
        this.type = IDENTITY_TYPE;
    }

    @JacocoGenerated
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JacocoGenerated
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JacocoGenerated
    public void setName(String name) {
        this.name = name;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(name, type);
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
        return Objects.equals(name, identity.name) && Objects.equals(type, identity.type);
    }
}
