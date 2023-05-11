package no.unit.nva.publication.model.business;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;

@SuppressWarnings("PMD.TooManyFields")
public final class ResourceBuilder {

    private SortableIdentifier identifier;
    private PublicationStatus status;
    private Owner resourceOwner;
    private Organization publisher;
    private Instant createdDate;
    private Instant modifiedDate;
    private Instant publishedDate;
    private Instant indexedDate;
    private URI link;
    private AssociatedArtifactList associatedArtifacts;
    private List<ResearchProject> projects;
    private EntityDescription entityDescription;
    private URI doi;
    private URI handle;
    private Set<AdditionalIdentifier> additionalIdentifiers;
    private List<URI> subjects;
    private List<Funding> fundings;
    private String rightsHolder;
    private ImportStatus importStatus;

    ResourceBuilder() {
    }

    public ResourceBuilder withIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
        return this;
    }

    public ResourceBuilder withStatus(PublicationStatus status) {
        this.status = status;
        return this;
    }

    public ResourceBuilder withResourceOwner(Owner resourceOwner) {
        this.resourceOwner = resourceOwner;
        return this;
    }

    public ResourceBuilder withPublisher(Organization publisher) {
        this.publisher = publisher;
        return this;
    }

    public ResourceBuilder withCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public ResourceBuilder withModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
        return this;
    }

    public ResourceBuilder withPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
        return this;
    }

    public ResourceBuilder withIndexedDate(Instant indexedDate) {
        this.indexedDate = indexedDate;
        return this;
    }

    public ResourceBuilder withLink(URI link) {
        this.link = link;
        return this;
    }

    public ResourceBuilder withAssociatedArtifactsList(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = new AssociatedArtifactList(associatedArtifacts);
        return this;
    }

    public ResourceBuilder withProjects(List<ResearchProject> projects) {
        this.projects = projects;
        return this;
    }

    public ResourceBuilder withEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
        return this;
    }

    public ResourceBuilder withDoi(URI doi) {
        this.doi = doi;
        return this;
    }

    public ResourceBuilder withHandle(URI handle) {
        this.handle = handle;
        return this;
    }

    public ResourceBuilder withAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
        return this;
    }

    public ResourceBuilder withSubjects(List<URI> subjects) {
        this.subjects = subjects;
        return this;
    }

    public ResourceBuilder withFundings(List<Funding> fundings) {
        this.fundings = fundings;
        return this;
    }

    public ResourceBuilder withRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
        return this;
    }

    public ResourceBuilder withImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
        return this;
    }

    public Resource build() {
        Resource resource = new Resource();
        resource.setIdentifier(identifier);
        resource.setStatus(status);
        resource.setResourceOwner(resourceOwner);
        resource.setPublisher(publisher);
        resource.setCreatedDate(createdDate);
        resource.setModifiedDate(modifiedDate);
        resource.setPublishedDate(publishedDate);
        resource.setIndexedDate(indexedDate);
        resource.setLink(link);
        resource.setAssociatedArtifacts(associatedArtifacts);
        resource.setProjects(projects);
        resource.setEntityDescription(entityDescription);
        resource.setDoi(doi);
        resource.setHandle(handle);
        resource.setAdditionalIdentifiers(additionalIdentifiers);
        resource.setSubjects(subjects);
        resource.setFundings(fundings);
        resource.setRightsHolder(rightsHolder);
        resource.setImportStatus(importStatus);
        return resource;
    }
}
