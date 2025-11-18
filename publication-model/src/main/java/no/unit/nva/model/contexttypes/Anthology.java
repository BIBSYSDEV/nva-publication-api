package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Anthology implements PublicationContext {

    @JsonAlias("partOf")
    private URI id;

    public Anthology() {
    }

    private Anthology(Builder builder) {
        id = builder.id;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Anthology)) {
            return false;
        }
        Anthology anthology = (Anthology) o;
        return Objects.equals(getId(), anthology.getId());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public static final class Builder {
        private URI id;

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Anthology build() {
            return new Anthology(this);
        }
    }
}
