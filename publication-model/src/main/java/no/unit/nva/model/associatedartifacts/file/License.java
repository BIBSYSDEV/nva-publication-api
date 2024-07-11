package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class License {

    private String identifier;
    private Map<String, String> labels;
    private URI link;

    @JacocoGenerated
    public License() {

    }

    private License(Builder builder) {
        setIdentifier(builder.identifier);
        setLabels(builder.labels);
        setLink(builder.link);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, String> getLabels() {
        return Objects.nonNull(labels) ? labels : Collections.emptyMap();
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public URI getLink() {
        return link;
    }

    public void setLink(URI link) {
        this.link = link;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getLabels(), getLink());
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
        License license = (License) o;
        return Objects.equals(getIdentifier(), license.getIdentifier())
               && Objects.equals(getLabels(), license.getLabels())
               && Objects.equals(getLink(), license.getLink());
    }

    public static final class Builder {

        private String identifier;
        private Map<String, String> labels;
        private URI link;

        public Builder() {
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withLabels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder withLink(URI link) {
            this.link = link;
            return this;
        }

        public License build() {
            return new License(this);
        }
    }
}
