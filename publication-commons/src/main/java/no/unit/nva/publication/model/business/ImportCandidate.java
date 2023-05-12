package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields", "PMD.GodClass"})
public class ImportCandidate extends Publication {

    private ImportStatus importStatus;

    @Override
    public PublicationStatus getStatus() {
        return null;
    }

    public ImportCandidate() {
        super();
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    public Publication toPublication() {
        return this.copy().build();
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        if (!super.equals(o)) {return false;}
        ImportCandidate that = (ImportCandidate) o;
        return getImportStatus() == that.getImportStatus();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getImportStatus());
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

        public Builder withFundings(List<Funding> fundings) {
            importCandidate.setFundings(fundings);
            return this;
        }

        public Builder withAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
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

        public Builder withRightsHolder(String rightsHolder) {
            this.importCandidate.setRightsHolder(rightsHolder);
            return this;
        }

        public ImportCandidate build() {
            return importCandidate;
        }

    }
}
