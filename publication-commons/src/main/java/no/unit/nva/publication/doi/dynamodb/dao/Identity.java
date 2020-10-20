package no.unit.nva.publication.doi.dynamodb.dao;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

import nva.commons.utils.JacocoGenerated;

public class Identity {

    private final String name;
    private final String arpId;
    private final String orcId;

    @JacocoGenerated
    @JsonCreator
    public Identity(@JsonProperty("arpId") String arpId,
                    @JsonProperty("orcId") String orcId,
                    @JsonProperty("name") String name) {
        this.name = name;
        this.arpId = arpId;
        this.orcId = orcId;
    }

    private Identity(Builder builder) {
        orcId = builder.orcId;
        arpId = builder.arpId;
        name = builder.name;
    }

    @JacocoGenerated
    public String getArpId() {
        return arpId;
    }

    @JacocoGenerated
    public String getOrcId() {
        return orcId;
    }

    @JacocoGenerated
    public String getName() {
        return name;
    }

    public static final class Builder {

        private String orcId;
        private String arpId;
        private String name;


        public Builder() {
        }

        public Builder withOrcId(String orcId) {
            this.orcId = orcId;
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

        public Identity build() {
            return new Identity(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Identity identity = (Identity) o;
        return Objects.equals(name, identity.name)
            && Objects.equals(arpId, identity.arpId)
            && Objects.equals(orcId, identity.orcId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arpId, orcId);
    }
}
