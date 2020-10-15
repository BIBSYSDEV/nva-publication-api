package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

public class Contributor {

    private final URI id;
    private final String name;

    @JacocoGenerated
    @JsonCreator
    public Contributor(@JsonProperty("id") URI id,
                       @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    private Contributor(Builder builder) {
        id = builder.id;
        name = builder.name;
    }

    @JacocoGenerated
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    public String getName() {
        return name;
    }

    public static final class Builder {

        private URI id;
        private String name;

        public Builder() {
        }

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Contributor build() {
            return new Contributor(this);
        }
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Contributor)) {
            return false;
        }
        Contributor that = (Contributor) o;
        return Objects.equals(getId(), that.getId())
            && Objects.equals(getName(), that.getName());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName());
    }
}
