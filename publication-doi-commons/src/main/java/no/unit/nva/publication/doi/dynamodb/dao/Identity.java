package no.unit.nva.publication.doi.dynamodb.dao;

import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.CONTRIBUTOR_ARP_ID_JSON_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.CONTRIBUTOR_NAME_JSON_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.CONTRIBUTOR_ORC_ID_JSON_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.textFromNode;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

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

    public static final class Builder {

        private String orcId;
        private String arpId;
        private String name;

        public Builder() {
        }

        /**
         * Extracts and populates orcId, arpId and name from a identity.
         *
         * @param identity json node pointing at a entry under
         * {@link DynamodbStreamRecordJsonPointers#CONTRIBUTORS_LIST_POINTER}.
         * @return Builder
         */
        public Builder withJsonNode(JsonNode identity) {
            orcId = textFromNode(identity, CONTRIBUTOR_ORC_ID_JSON_POINTER);
            arpId = textFromNode(identity, CONTRIBUTOR_ARP_ID_JSON_POINTER);
            name = textFromNode(identity, CONTRIBUTOR_NAME_JSON_POINTER);
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
}
