package no.unit.nva.model;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Identity {

    private URI id;
    private String name;
    private NameType nameType;
    private String orcId;
    private ContributorVerificationStatus verificationStatus;
    private List<AdditionalIdentifier> additionalIdentifiers;

    public Identity() {
    }

    private Identity(Builder builder) {
        setId(builder.id);
        setName(builder.name);
        setNameType(builder.nameType);
        setOrcId(builder.orcId);
        setVerificationStatus(builder.verificationStatus);
        setAdditionalIdentifiers(builder.additionalIdentifiers);
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NameType getNameType() {
        return nameType;
    }

    public void setNameType(NameType nameType) {
        this.nameType = nameType;
    }

    public String getOrcId() {
        return orcId;
    }

    public void setOrcId(String orcId) {
        this.orcId = orcId;
    }

    public List<AdditionalIdentifier> getAdditionalIdentifiers() {
        return nonNull(additionalIdentifiers)
                   ? additionalIdentifiers
                   : emptyList();
    }

    public void setAdditionalIdentifiers(List<AdditionalIdentifier> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    public ContributorVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(ContributorVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(),
                            getName(),
                            getNameType(),
                            getOrcId(),
                            getVerificationStatus(),
                            getAdditionalIdentifiers());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Identity)) {
            return false;
        }
        Identity identity = (Identity) o;
        return Objects.equals(getId(), identity.getId())
               && Objects.equals(getName(), identity.getName())
               && getNameType() == identity.getNameType()
               && Objects.equals(getOrcId(), identity.getOrcId())
               && Objects.equals(getAdditionalIdentifiers(), identity.getAdditionalIdentifiers())
               && Objects.equals(getVerificationStatus(), identity.getVerificationStatus());
    }

    public static final class Builder {

        private URI id;
        private String name;
        private NameType nameType;
        private String orcId;

        private List<AdditionalIdentifier> additionalIdentifiers;

        private ContributorVerificationStatus verificationStatus;

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

        public Builder withNameType(NameType nameType) {
            this.nameType = nameType;
            return this;
        }

        public Builder withOrcId(String orcId) {
            this.orcId = orcId;
            return this;
        }

        public Builder withVerificationStatus(ContributorVerificationStatus verificationStatus) {
            this.verificationStatus = verificationStatus;
            return this;
        }

        public Builder withAdditionalIdentifiers(List<AdditionalIdentifier> additionalIdentifiers) {
            this.additionalIdentifiers = additionalIdentifiers;
            return this;
        }

        public Identity build() {
            return new Identity(this);
        }
    }
}
