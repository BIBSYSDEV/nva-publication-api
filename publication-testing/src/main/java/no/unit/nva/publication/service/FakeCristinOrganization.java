package no.unit.nva.publication.service;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import no.unit.nva.commons.json.JsonSerializable;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

public record FakeCristinOrganization(
        @JsonProperty("id") URI id,
        @JsonProperty("acronym") String acronym,
        @JsonProperty("country") String countryCode,
        @JsonProperty("partOf") Collection<FakeCristinOrganization> partOf,
        @JsonProperty("hasPart") Collection<FakeCristinOrganization> hasPart,
        @JsonProperty("labels") Map<String, String> labels,
        @JsonProperty("type") String type,
        @JsonProperty("@context") String context)
        implements JsonSerializable {

    public static FakeCristinOrganization from(String json) throws JsonProcessingException {
        return dtoObjectMapper.readValue(json, FakeCristinOrganization.class);
    }

    public static FakeCristinOrganization asLeafNode(URI organizationId) {
        return builder().withId(organizationId).build();
    }

    @Override
    public String toJsonString() {
        return attempt(() -> dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private URI id;
        private Collection<FakeCristinOrganization> parentOrganizations;
        private Collection<FakeCristinOrganization> subOrganizations;
        private Map<String, String> labels;
        private String acronym;
        private String context;
        private String countryCode;
        private String type;

        private Builder() {}

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withAcronym(String acronym) {
            this.acronym = acronym;
            return this;
        }

        public Builder withCountryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder withPartOf(Collection<FakeCristinOrganization> partOf) {
            this.parentOrganizations = partOf;
            return this;
        }

        public Builder withHasPart(Collection<FakeCristinOrganization> hasPart) {
            this.subOrganizations = hasPart;
            return this;
        }

        public Builder withLabels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withContext(String context) {
            this.context = context;
            return this;
        }

        public FakeCristinOrganization build() {
            return new FakeCristinOrganization(
                    id,
                    acronym,
                    countryCode,
                    parentOrganizations,
                    subOrganizations,
                    labels,
                    type,
                    context);
        }
    }
}
