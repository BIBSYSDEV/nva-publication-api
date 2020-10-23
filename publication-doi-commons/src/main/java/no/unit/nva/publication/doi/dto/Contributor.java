package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

public class Contributor {

    private final URI id;
    private final String arpId;
    private final String name;

    /**
     * Constructs a Contributor for doi.publisher DTO
     *
     * @param id    URI for our contributor. Null atm.
     * @param arpId Authority register id for a person for this contributor entry.
     * @param name  name of contributor
     */
    @JacocoGenerated
    @JsonCreator
    public Contributor(@JsonProperty("id") URI id,
                       @JsonProperty("arpId") String arpId,
                       @JsonProperty("name") String name) {
        this.id = id;
        this.arpId = arpId;
        this.name = name;
    }

    private Contributor(Builder builder) {
        this(builder.id, builder.arpId, builder.name);
    }

    @JacocoGenerated
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    public String getArpId() {
        return arpId;
    }

    @JacocoGenerated
    public String getName() {
        return name;
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
        Contributor that = (Contributor) o;
        return Objects.equals(id, that.id)
            && Objects.equals(arpId, that.arpId)
            && Objects.equals(name, that.name);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(id, arpId, name);
    }

    public static final class Builder {

        private URI id;
        private String name;
        private String arpId;

        public Builder() {
        }

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withArpId(String arpId) {
            this.arpId = arpId;
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
}
