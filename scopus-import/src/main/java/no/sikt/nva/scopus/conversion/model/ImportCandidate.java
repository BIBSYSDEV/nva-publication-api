package no.sikt.nva.scopus.conversion.model;

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
        return new Publication.Builder()
                .withIdentifier(getIdentifier())
                .withStatus(PublicationStatus.PUBLISHED)
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
                .withRightsHolder(getRightsHolder())
                .build();
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

        public ImportCandidate.Builder withImportStatus(ImportStatus importStatus) {
            importCandidate.setImportStatus(importStatus);
            return this;
        }

        public ImportCandidate.Builder withIdentifier(SortableIdentifier identifier) {
            importCandidate.setIdentifier(identifier);
            return this;
        }

        public ImportCandidate.Builder withStatus(PublicationStatus status) {
            importCandidate.setStatus(status);
            return this;
        }

        public ImportCandidate.Builder withPublisher(Organization publisher) {
            importCandidate.setPublisher(publisher);
            return this;
        }

        public ImportCandidate.Builder withCreatedDate(Instant createdDate) {
            importCandidate.setCreatedDate(createdDate);
            return this;
        }

        public ImportCandidate.Builder withModifiedDate(Instant modifiedDate) {
            importCandidate.setModifiedDate(modifiedDate);
            return this;
        }

        public ImportCandidate.Builder withPublishedDate(Instant publishedDate) {
            importCandidate.setPublishedDate(publishedDate);
            return this;
        }

        public ImportCandidate.Builder withIndexedDate(Instant indexedDate) {
            importCandidate.setIndexedDate(indexedDate);
            return this;
        }

        public ImportCandidate.Builder withHandle(URI handle) {
            importCandidate.setHandle(handle);
            return this;
        }

        public ImportCandidate.Builder withDoi(URI doi) {
            importCandidate.setDoi(doi);
            return this;
        }

        public ImportCandidate.Builder withLink(URI link) {
            importCandidate.setLink(link);
            return this;
        }

        public ImportCandidate.Builder withEntityDescription(EntityDescription entityDescription) {
            importCandidate.setEntityDescription(entityDescription);
            return this;
        }

        public ImportCandidate.Builder withAssociatedArtifacts(List<AssociatedArtifact> associatedArtifacts) {
            importCandidate.setAssociatedArtifacts(new AssociatedArtifactList(associatedArtifacts));
            return this;
        }

        public ImportCandidate.Builder withProjects(List<ResearchProject> projects) {
            importCandidate.setProjects(projects);
            return this;
        }

        public ImportCandidate.Builder withFundings(List<Funding> fundings) {
            importCandidate.setFundings(fundings);
            return this;
        }

        public ImportCandidate.Builder withAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
            importCandidate.setAdditionalIdentifiers(additionalIdentifiers);
            return this;
        }

        public ImportCandidate.Builder withSubjects(List<URI> subjects) {
            importCandidate.setSubjects(subjects);
            return this;
        }

        public ImportCandidate.Builder withResourceOwner(ResourceOwner randomResourceOwner) {
            importCandidate.setResourceOwner(randomResourceOwner);
            return this;
        }

        public ImportCandidate.Builder withRightsHolder(String rightsHolder) {
            this.importCandidate.setRightsHolder(rightsHolder);
            return this;
        }

        public ImportCandidate build() {
            return importCandidate;
        }

    }
}
