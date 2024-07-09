package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UnconfirmedOrganization extends Corporation {

    public static final String NAME = "name";

    @JsonProperty(NAME)
    private final String name;

    @JsonCreator
    public UnconfirmedOrganization(@JsonProperty(NAME) String name) {
        super();
        this.name = name;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnconfirmedOrganization)) {
            return false;
        }
        UnconfirmedOrganization that = (UnconfirmedOrganization) o;
        return Objects.equals(name, that.name);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
