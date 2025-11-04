package no.unit.nva.publication.model.business.importcandidate;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields", "PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class ImportCandidate implements JsonSerializable {

    public static final String TYPE = "ImportCandidate";
    private static final String IMPORT_STATUS = "importStatus";
    private static final String ASSOCIATED_CUSTOMERS_FIELD = "associatedCustomers";
    @JsonProperty(IMPORT_STATUS)
    private ImportStatus importStatus;
    @JsonProperty(ASSOCIATED_CUSTOMERS_FIELD)
    private List<URI> associatedCustomers;
    private SortableIdentifier identifier;
    private ResourceOwner resourceOwner;
    private Organization publisher;
    private Instant createdDate;
    private Instant modifiedDate;
    private EntityDescription entityDescription;
    private AssociatedArtifactList associatedArtifacts;
    private Set<AdditionalIdentifierBase> additionalIdentifiers;

    public ImportCandidate() {
    }

    public Set<AdditionalIdentifierBase> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    public void setAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public PublicationStatus getStatus() {
        return null;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public ResourceOwner getResourceOwner() {
        return resourceOwner;
    }

    public void setResourceOwner(ResourceOwner resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    public Organization getPublisher() {
        return publisher;
    }

    public void setPublisher(Organization publisher) {
        this.publisher = publisher;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    public AssociatedArtifactList getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    public void setAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImportCandidate that)) {
            return false;
        }
        return Objects.equals(getImportStatus(), that.getImportStatus())
               && Objects.equals(getAssociatedCustomers(), that.getAssociatedCustomers())
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getResourceOwner(), that.getResourceOwner())
               && Objects.equals(getPublisher(), that.getPublisher())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getEntityDescription(), that.getEntityDescription())
               && Objects.equals(getAdditionalIdentifiers(), that.getAdditionalIdentifiers())
               && new HashSet<>(getAssociatedArtifacts()).containsAll(that.getAssociatedArtifacts());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getImportStatus(), getAssociatedCustomers(), getIdentifier(), getResourceOwner(),
                            getPublisher(), getCreatedDate(), getModifiedDate(), getEntityDescription(),
                            getAssociatedArtifacts(), getAdditionalIdentifiers());
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }

    public Builder copy() {
        return new ImportCandidate.Builder()
                   .withIdentifier(getIdentifier())
                   .withResourceOwner(getResourceOwner())
                   .withPublisher(getPublisher())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withEntityDescription(getEntityDescription())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withAssociatedArtifacts(getAssociatedArtifacts())
                   .withImportStatus(getImportStatus())
                   .withAssociatedCustomers(getAssociatedCustomers());
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    public List<URI> getAssociatedCustomers() {
        return nonNull(associatedCustomers) ? associatedCustomers.stream().filter(Objects::nonNull).toList()
                   : Collections.emptyList();
    }

    public void setAssociatedCustomers(Collection<URI> associatedCustomers) {
        this.associatedCustomers =
            nonNull(associatedCustomers) ? associatedCustomers.stream().filter(Objects::nonNull).toList()
                : Collections.emptyList();
    }

    public Publication toPublication() {
        return new Publication.Builder()
                   .withIdentifier(getIdentifier())
                   .withPublisher(getPublisher())
                   .withResourceOwner(getResourceOwner())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withEntityDescription(getEntityDescription())
                   .withAssociatedArtifacts(getAssociatedArtifacts())
                   .withStatus(PublicationStatus.PUBLISHED)
                   .build();
    }

    public Optional<String> getScopusIdentifier() {
        return getAdditionalIdentifiers().stream()
                   .filter(ScopusIdentifier.class::isInstance)
                   .map(ScopusIdentifier.class::cast)
                   .map(ScopusIdentifier::value)
                   .findFirst();
    }

    public static final class Builder {

        private final ImportCandidate importCandidate;

        public Builder() {
            importCandidate = new ImportCandidate();
        }

        public Builder withImportStatus(ImportStatus importStatus) {
            importCandidate.setImportStatus(importStatus);
            return this;
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            importCandidate.setIdentifier(identifier);
            return this;
        }

        public Builder withPublisher(Organization publisher) {
            importCandidate.setPublisher(publisher);
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            importCandidate.setCreatedDate(createdDate);
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            importCandidate.setModifiedDate(modifiedDate);
            return this;
        }

        public Builder withEntityDescription(EntityDescription entityDescription) {
            importCandidate.setEntityDescription(entityDescription);
            return this;
        }

        public Builder withAssociatedArtifacts(List<AssociatedArtifact> associatedArtifacts) {
            importCandidate.setAssociatedArtifacts(new AssociatedArtifactList(associatedArtifacts));
            return this;
        }

        public Builder withAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
            importCandidate.setAdditionalIdentifiers(additionalIdentifiers);
            return this;
        }

        public Builder withResourceOwner(ResourceOwner randomResourceOwner) {
            importCandidate.setResourceOwner(randomResourceOwner);
            return this;
        }

        public Builder withAssociatedCustomers(Collection<URI> associatedCustomers) {
            this.importCandidate.setAssociatedCustomers(associatedCustomers);
            return this;
        }

        public ImportCandidate build() {
            return importCandidate;
        }
    }
}
