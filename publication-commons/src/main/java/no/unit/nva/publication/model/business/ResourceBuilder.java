package no.unit.nva.publication.model.business;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;

public final class ResourceBuilder {
    
    private final Resource resource;
    
    protected ResourceBuilder() {
        resource = new Resource();
    }
    
    public ResourceBuilder withIdentifier(SortableIdentifier identifier) {
        resource.setIdentifier(identifier);
        return this;
    }
    
    public ResourceBuilder withStatus(PublicationStatus status) {
        resource.setStatus(status);
        return this;
    }
    
    public ResourceBuilder withResourceOwner(Owner resourceOwner) {
        resource.setResourceOwner(resourceOwner);
        return this;
    }
    
    public ResourceBuilder withPublisher(Organization publisher) {
        resource.setPublisher(publisher);
        return this;
    }
    
    public ResourceBuilder withCreatedDate(Instant createdDate) {
        resource.setCreatedDate(createdDate);
        return this;
    }
    
    public ResourceBuilder withModifiedDate(Instant modifiedDate) {
        resource.setModifiedDate(modifiedDate);
        return this;
    }
    
    public ResourceBuilder withPublishedDate(Instant publishedDate) {
        resource.setPublishedDate(publishedDate);
        return this;
    }
    
    public ResourceBuilder withIndexedDate(Instant indexedDate) {
        resource.setIndexedDate(indexedDate);
        return this;
    }
    
    public ResourceBuilder withLink(URI link) {
        resource.setLink(link);
        return this;
    }
    
    public ResourceBuilder withAssociatedArtifactsList(AssociatedArtifactList associatedArtifacts) {
        resource.setAssociatedArtifacts(associatedArtifacts);
        return this;
    }
    
    public ResourceBuilder withProjects(List<ResearchProject> projects) {
        resource.setProjects(projects);
        return this;
    }
    
    public ResourceBuilder withEntityDescription(EntityDescription entityDescription) {
        resource.setEntityDescription(entityDescription);
        return this;
    }
    
    public ResourceBuilder withDoi(URI doi) {
        resource.setDoi(doi);
        return this;
    }
    
    public ResourceBuilder withHandle(URI handle) {
        resource.setHandle(handle);
        return this;
    }
    
    public ResourceBuilder withAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
        resource.setAdditionalIdentifiers(additionalIdentifiers);
        return this;
    }
    
    public ResourceBuilder withSubjects(List<URI> subjects) {
        resource.setSubjects(subjects);
        return this;
    }

    public ResourceBuilder withFundings(List<Funding> fundings) {
        resource.setFundings(fundings);
        return this;
    }

    public Resource build() {
        return resource;
    }
}
