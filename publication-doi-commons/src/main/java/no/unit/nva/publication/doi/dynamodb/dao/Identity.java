package no.unit.nva.publication.doi.dynamodb.dao;

import static no.unit.nva.publication.doi.JsonPointerUtils.textFromNode;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Identity {

    private final String name;
    private final String arpId;
    private final String orcId;

    protected Identity(Builder builder) {
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
        return Objects.equals(name, identity.name)
            && Objects.equals(arpId, identity.arpId)
            && Objects.equals(orcId, identity.orcId);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(name, arpId, orcId);
    }

    public static final class Builder {

        private String orcId;
        private String arpId;
        private String name;

        private final DynamodbStreamRecordJsonPointers jsonPointers;

        public Builder(DynamodbStreamRecordJsonPointers jsonPointers) {
            this.jsonPointers = jsonPointers;
        }

        /**
         * Extracts and populates orcId, arpId and name from a identity.
         *
         * @param identity json node pointing at a entry under
         * {@link DynamodbStreamRecordJsonPointers#getContributorsListJsonPointer()}.
         * @return Builder
         */
        public Builder withJsonNode(JsonNode identity) {
            orcId = textFromNode(identity, jsonPointers.getIdentityOrcIdJsonPointer());
            arpId = textFromNode(identity, jsonPointers.getIdentityArpIdJsonPointer());
            name = textFromNode(identity, jsonPointers.getIdentityNameJsonPointer());
            return this;
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
}
