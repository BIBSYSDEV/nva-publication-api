package no.unit.nva.publication.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.WithContext;
import no.unit.nva.WithFile;
import no.unit.nva.WithMetadata;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResearchProject;
import nva.commons.core.JacocoGenerated;

public class CreatePublicationRequest implements WithMetadata, WithFile, WithContext {
    
    private EntityDescription entityDescription;
    private FileSet fileSet;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<URI> subjects;
    private Set<AdditionalIdentifier> additionalIdentifiers;
    
    public static CreatePublicationRequest fromPublication(Publication publication) {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setEntityDescription(publication.getEntityDescription());
        createPublicationRequest.setFileSet(publication.getFileSet());
        createPublicationRequest.setProjects(publication.getProjects());
        createPublicationRequest.setSubjects(publication.getSubjects());
        createPublicationRequest.setAdditionalIdentifiers(publication.getAdditionalIdentifiers());
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
        publication.setFileSet(getFileSet());
        publication.setProjects(getProjects());
        publication.setSubjects(getSubjects());
        publication.setAdditionalIdentifiers(getAdditionalIdentifiers());
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
    public FileSet getFileSet() {
        return fileSet;
    }
    
    @Override
    @JacocoGenerated
    public void setFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
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
        return Objects.hash(getEntityDescription(), getFileSet(), getContext(), getProjects(), getSubjects(),
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
               && Objects.equals(getFileSet(), that.getFileSet())
               && Objects.equals(getContext(), that.getContext())
               && Objects.equals(getProjects(), that.getProjects())
               && Objects.equals(getSubjects(), that.getSubjects())
               && Objects.equals(getAdditionalIdentifiers(), that.getAdditionalIdentifiers());
    }
}
