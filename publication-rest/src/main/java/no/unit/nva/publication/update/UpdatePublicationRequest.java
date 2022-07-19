package no.unit.nva.publication.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import no.unit.nva.WithContext;
import no.unit.nva.WithFile;
import no.unit.nva.WithIdentifier;
import no.unit.nva.WithMetadata;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResearchProject;
import nva.commons.core.JacocoGenerated;

public class UpdatePublicationRequest implements WithIdentifier, WithMetadata, WithFile, WithContext {
    
    public static final String WRONG_PUBLICATION_UDPATE_ERROR = "Trying to update a publication with different "
                                                                + "identifier:";
    private SortableIdentifier identifier;
    private EntityDescription entityDescription;
    private FileSet fileSet;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<URI> subjects;
    
    public Publication generatePublicationUpdate(Publication existingPublication) {
        if (!this.identifier.equals(existingPublication.getIdentifier())) {
            throw new IllegalArgumentException(
                WRONG_PUBLICATION_UDPATE_ERROR + existingPublication.getIdentifier());
        }
        return existingPublication.copy()
            .withEntityDescription(this.entityDescription)
            .withFileSet(this.fileSet)
            .withProjects(this.projects)
            .withSubjects(this.subjects)
            .build();
    }
    
    @JacocoGenerated
    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    @JacocoGenerated
    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
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
    
    @Override
    @JacocoGenerated
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
    
    @Override
    @JacocoGenerated
    public void setContext(JsonNode context) {
        this.context = context;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(identifier, entityDescription, fileSet, subjects, context);
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdatePublicationRequest that = (UpdatePublicationRequest) o;
        return Objects.equals(identifier, that.identifier)
               && Objects.equals(entityDescription, that.entityDescription)
               && Objects.equals(fileSet, that.fileSet)
               && Objects.equals(subjects, that.subjects)
               && Objects.equals(context, that.context);
    }
}

