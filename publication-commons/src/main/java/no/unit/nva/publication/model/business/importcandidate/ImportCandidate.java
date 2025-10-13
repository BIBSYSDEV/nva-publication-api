package no.unit.nva.publication.model.business.importcandidate;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationNoteBase;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields", "PMD.GodClass"})
public class ImportCandidate extends Publication implements JsonSerializable {

    public static final String TYPE = "ImportCandidate";
    private static final String IMPORT_STATUS = "importStatus";
    private static final String ASSOCIATED_CUSTOMERS_FIELD = "associatedCustomers";
    @JsonProperty(IMPORT_STATUS)
    private ImportStatus importStatus;
    @JsonProperty(ASSOCIATED_CUSTOMERS_FIELD)
    private List<URI> associatedCustomers;

    public ImportCandidate() {
        super();
    }

    public Builder copyImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(getImportStatus())
                   .withIdentifier(getIdentifier())
                   .withStatus(getStatus())
                   .withResourceOwner(getResourceOwner())
                   .withPublisher(getPublisher())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withPublishedDate(getPublishedDate())
                   .withIndexedDate(getIndexedDate())
                   .withHandle(getHandle())
                   .withDoi(getDoi())
                   .withLink(getLink())
                   .withEntityDescription(getEntityDescription())
                   .withProjects(getProjects())
                   .withFundings(getFundings())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withAssociatedArtifacts(getAssociatedArtifacts())
                   .withSubjects(getSubjects())
                   .withFundings(getFundings())
                   .withPublicationNotes(getPublicationNotes())
                   .withImportStatus(getImportStatus())
                   .withRightsHolder(getRightsHolder())
                   .withCuratingInstitutions(getCuratingInstitutions())
                   .withAssociatedCustomers(getAssociatedCustomers())
                   .withImportDetails(getImportDetails());
    }

    @Override
    public PublicationStatus getStatus() {
        return null;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImportCandidate that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(getImportStatus(), that.getImportStatus()) && Objects.equals(
            getAssociatedCustomers(), that.getAssociatedCustomers());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getImportStatus(), getAssociatedCustomers());
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    public Collection<URI> getAssociatedCustomers() {
        return nonNull(associatedCustomers)
                   ? associatedCustomers.stream().filter(Objects::nonNull).toList()
                   : new ArrayList<>();
    }

    public void setAssociatedCustomers(Collection<URI> associatedCustomers) {
        this.associatedCustomers = nonNull(associatedCustomers)
                                       ? new ArrayList<>(associatedCustomers)
                                       : new ArrayList<>();
    }

    public Publication toPublication() {
        return this.copy()
                   .withStatus(PublicationStatus.PUBLISHED)
                   .build();
    }

    public static final class Builder {

        private final ImportCandidate importCandidate;

        public Builder() {
            importCandidate = new ImportCandidate();
        }

        public Builder withPublication(Publication publication) {
            importCandidate.setIdentifier(publication.getIdentifier());
            importCandidate.setStatus(publication.getStatus());
            importCandidate.setPublisher(publication.getPublisher());
            importCandidate.setCreatedDate(publication.getCreatedDate());
            importCandidate.setModifiedDate(publication.getModifiedDate());
            importCandidate.setPublishedDate(publication.getPublishedDate());
            importCandidate.setIndexedDate(publication.getIndexedDate());
            importCandidate.setHandle(publication.getHandle());
            importCandidate.setDoi(publication.getDoi());
            importCandidate.setLink(publication.getLink());
            importCandidate.setEntityDescription(publication.getEntityDescription());
            importCandidate.setAssociatedArtifacts(publication.getAssociatedArtifacts());
            importCandidate.setProjects(publication.getProjects());
            importCandidate.setFundings(publication.getFundings());
            importCandidate.setAdditionalIdentifiers(publication.getAdditionalIdentifiers());
            importCandidate.setSubjects(publication.getSubjects());
            importCandidate.setResourceOwner(publication.getResourceOwner());
            importCandidate.setRightsHolder(publication.getRightsHolder());
            importCandidate.setPublicationNotes(publication.getPublicationNotes());
            importCandidate.setDuplicateOf(publication.getDuplicateOf());
            importCandidate.setCuratingInstitutions(publication.getCuratingInstitutions());
            importCandidate.setImportDetails(publication.getImportDetails());
            return this;
        }

        public Builder withImportStatus(ImportStatus importStatus) {
            importCandidate.setImportStatus(importStatus);
            return this;
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            importCandidate.setIdentifier(identifier);
            return this;
        }

        public Builder withStatus(PublicationStatus status) {
            importCandidate.setStatus(status);
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

        public Builder withPublishedDate(Instant publishedDate) {
            importCandidate.setPublishedDate(publishedDate);
            return this;
        }

        public Builder withIndexedDate(Instant indexedDate) {
            importCandidate.setIndexedDate(indexedDate);
            return this;
        }

        public Builder withHandle(URI handle) {
            importCandidate.setHandle(handle);
            return this;
        }

        public Builder withDoi(URI doi) {
            importCandidate.setDoi(doi);
            return this;
        }

        public Builder withLink(URI link) {
            importCandidate.setLink(link);
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

        public Builder withProjects(List<ResearchProject> projects) {
            importCandidate.setProjects(projects);
            return this;
        }

        public Builder withFundings(Set<Funding> fundings) {
            importCandidate.setFundings(fundings);
            return this;
        }

        public Builder withAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
            importCandidate.setAdditionalIdentifiers(additionalIdentifiers);
            return this;
        }

        public Builder withSubjects(List<URI> subjects) {
            importCandidate.setSubjects(subjects);
            return this;
        }

        public Builder withResourceOwner(ResourceOwner randomResourceOwner) {
            importCandidate.setResourceOwner(randomResourceOwner);
            return this;
        }

        public Builder withPublicationNotes(List<PublicationNoteBase> publicationNotes) {
            this.importCandidate.setPublicationNotes(publicationNotes);
            return this;
        }

        public Builder withRightsHolder(String rightsHolder) {
            this.importCandidate.setRightsHolder(rightsHolder);
            return this;
        }

        public Builder withDuplicateOf(URI duplicateOf) {
            this.importCandidate.setDuplicateOf(duplicateOf);
            return this;
        }

        public Builder withCuratingInstitutions(Set<CuratingInstitution> curatingInstitutions) {
            this.importCandidate.setCuratingInstitutions(curatingInstitutions);
            return this;
        }

        public Builder withImportDetails(List<ImportDetail> importDetails) {
            this.importCandidate.setImportDetails(importDetails);
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
