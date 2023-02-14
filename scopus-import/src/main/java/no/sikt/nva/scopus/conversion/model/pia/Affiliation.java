package no.sikt.nva.scopus.conversion.model.pia;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Affiliation {

    @JsonProperty("cristin_institution_id")
    private final String institutionIdentifier;
    @JsonProperty("cristin_unit_id")
    private final String unitIdentifier;
    @JsonProperty("count")
    private final String count;

    @JsonCreator
    public Affiliation(@JsonProperty("cristin_institution_id") String institutionIdentifier,
                       @JsonProperty("cristin_unit_id") String unitIdentifier,
                       @JsonProperty("count") String count) {
        this.institutionIdentifier = institutionIdentifier;
        this.unitIdentifier = unitIdentifier;
        this.count = count;
    }

    public Affiliation(Builder builder) {
        this(
            builder.institutionIdentifier,
            builder.unitIdentifier,
            builder.count
        );
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getInstitutionIdentifier(), getUnitIdentifier(), getCount());
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
        Affiliation that = (Affiliation) o;
        return Objects.equals(getInstitutionIdentifier(), that.getInstitutionIdentifier())
               && Objects.equals(getUnitIdentifier(), that.getUnitIdentifier())
               && Objects.equals(getCount(), that.getCount());
    }

    public String getInstitutionIdentifier() {
        return institutionIdentifier;
    }

    public String getUnitIdentifier() {
        return unitIdentifier;
    }

    public String getCount() {
        return count;
    }

    public static final class Builder {

        private String institutionIdentifier;
        private String unitIdentifier;
        private String count;

        public Builder() {
        }

        public Affiliation.Builder withInstitution(String institutionIdentifier) {
            this.institutionIdentifier = institutionIdentifier;
            return this;
        }

        public Affiliation.Builder withUnit(String unitIdentifier) {
            this.unitIdentifier = unitIdentifier;
            return this;
        }

        public Affiliation.Builder withCount(String count) {
            this.count = count;
            return this;
        }

        public Affiliation build() {
            return new Affiliation(this);
        }
    }
}
