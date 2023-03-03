package no.unit.nva.publication.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.unit.nva.WithAssociatedArtifact;
import no.unit.nva.WithContext;
import no.unit.nva.WithMetadata;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import nva.commons.core.JacocoGenerated;

public class CreatePublicationRequest implements WithMetadata, WithAssociatedArtifact, WithContext {
    
    private EntityDescription entityDescription;
    private AssociatedArtifactList associatedArtifacts;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<URI> subjects;
    private Set<AdditionalIdentifier> additionalIdentifiers;
    private List<Funding> fundings;
    private String rightsHolder;

    public static CreatePublicationRequest fromPublication(Publication publication) {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setEntityDescription(publication.getEntityDescription());
        createPublicationRequest.setAssociatedArtifacts(publication.getAssociatedArtifacts());
        createPublicationRequest.setProjects(publication.getProjects());
        createPublicationRequest.setSubjects(publication.getSubjects());
        createPublicationRequest.setAdditionalIdentifiers(publication.getAdditionalIdentifiers());
        createPublicationRequest.setFundings(publication.getFundings());
        createPublicationRequest.setRightsHolder(publication.getRightsHolder());
        return createPublicationRequest;
    }
    
    public Set<AdditionalIdentifier> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }
    
    public void setAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }
    
    public Publication toPublication() {
        Publication publication = new Publication();
        publication.setEntityDescription(getEntityDescription());
        publication.setAssociatedArtifacts(getAssociatedArtifacts());
        publication.setProjects(getProjects());
        publication.setSubjects(getSubjects());
        publication.setAdditionalIdentifiers(getAdditionalIdentifiers());
        publication.setFundings(getFundings());
        publication.setRightsHolder(getRightsHolder());
        return publication;
    }
    
    @JacocoGenerated
    @Override
    public EntityDescription getEntityDescription() {
        return entityDescription;
    }
    
    @JacocoGenerated
    @Override
    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }
    
    @JacocoGenerated
    @Override
    public List<ResearchProject> getProjects() {
        return projects;
    }
    
    @Override
    @JacocoGenerated
    public void setProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }
    
    @Override
    @JacocoGenerated
    public List<URI> getSubjects() {
        return subjects;
    }
    
    @Override
    @JacocoGenerated
    public void setSubjects(List<URI> subjects) {
        this.subjects = subjects;
    }
    
    @Override
    @JacocoGenerated
    public AssociatedArtifactList getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    @Override
    @JacocoGenerated
    public void setAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    @Override
    @JacocoGenerated
    public List<Funding> getFundings() {
        return fundings;
    }

    @Override
    @JacocoGenerated
    public void setFundings(List<Funding> fundings) {
        this.fundings = fundings;
    }

    @Override
    public void setRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
    }

    @Override
    public String getRightsHolder() {
        return rightsHolder;
    }

    @Override
    @JacocoGenerated
    public JsonNode getContext() {
        return context;
    }

    @JacocoGenerated
    @Override
    public void setContext(JsonNode context) {
        this.context = context;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getEntityDescription(), getAssociatedArtifacts(), getContext(), getProjects(), getSubjects(),
            getAdditionalIdentifiers());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreatePublicationRequest)) {
            return false;
        }
        CreatePublicationRequest that = (CreatePublicationRequest) o;
        return Objects.equals(getEntityDescription(), that.getEntityDescription())
               && Objects.equals(getAssociatedArtifacts(), that.getAssociatedArtifacts())
               && Objects.equals(getContext(), that.getContext())
               && Objects.equals(getProjects(), that.getProjects())
               && Objects.equals(getSubjects(), that.getSubjects())
               && Objects.equals(getAdditionalIdentifiers(), that.getAdditionalIdentifiers());
    }

}
