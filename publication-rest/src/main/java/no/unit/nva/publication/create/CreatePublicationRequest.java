package no.unit.nva.publication.create;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.net.URI;
import java.util.*;

import no.unit.nva.WithContext;
import no.unit.nva.WithMetadata;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationNoteBase;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.publication.sanitizer.EntityDescriptionSanitizer;
import no.unit.nva.publication.sanitizer.HtmlSanitizer;
import nva.commons.core.JacocoGenerated;

public class CreatePublicationRequest implements WithMetadata, WithContext {

    @Valid
    private EntityDescription entityDescription;
    private AssociatedArtifactList associatedArtifacts;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<URI> subjects;
    private Set<AdditionalIdentifierBase> additionalIdentifiers;
    private List<Funding> fundings;
    @Size(min = 1, max = 256)
    @Pattern(regexp = "^[\\p{L}\\d][\\p{L}\\d\\s]*\\S$")
    private String rightsHolder;
    private PublicationStatus status;
    private List<ImportDetail> importDetails;

    private List<PublicationNoteBase> publicationNotes;
    private URI duplicateOf;

    public static CreatePublicationRequest fromPublication(Publication publication) {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setEntityDescription(publication.getEntityDescription());
        createPublicationRequest.setAssociatedArtifacts(publication.getAssociatedArtifacts());
        createPublicationRequest.setProjects(publication.getProjects());
        createPublicationRequest.setSubjects(publication.getSubjects());
        createPublicationRequest.setAdditionalIdentifiers(publication.getAdditionalIdentifiers());
        createPublicationRequest.setFundings(publication.getFundings());
        createPublicationRequest.setRightsHolder(publication.getRightsHolder());
        createPublicationRequest.setStatus(publication.getStatus());
        createPublicationRequest.setPublicationNotes(publication.getPublicationNotes());
        createPublicationRequest.setDuplicateOf(publication.getDuplicateOf());
        createPublicationRequest.setImportDetails(publication.getImportDetails());
        return createPublicationRequest;
    }

    public Set<AdditionalIdentifierBase> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    public void setAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
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
        publication.setStatus(getStatus());
        publication.setDuplicateOf(getDuplicateOf());
        publication.setPublicationNotes(getPublicationNotes());
        publication.setImportDetails(getImportDetails());
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
        this.entityDescription = EntityDescriptionSanitizer.sanitize(entityDescription);
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

    @JacocoGenerated
    public AssociatedArtifactList getAssociatedArtifacts() {
        return associatedArtifacts != null ? associatedArtifacts : AssociatedArtifactList.empty();
    }

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

    @Override
    @JacocoGenerated
    public List<ImportDetail> getImportDetails() {
        return nonNull(importDetails) ? importDetails : Collections.emptyList();
    }

    @JacocoGenerated
    @Override
    public void setImportDetails(Collection<ImportDetail> importDetails) {
        this.importDetails = new ArrayList<>(importDetails);
    }

    @JacocoGenerated
    public PublicationStatus getStatus() {
        return status;
    }

    @JacocoGenerated
    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public List<PublicationNoteBase> getPublicationNotes() {
        return publicationNotes;
    }

    public URI getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(URI duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public void setPublicationNotes(List<PublicationNoteBase> publicationNotes) {
        this.publicationNotes = publicationNotes;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getEntityDescription(),
                getAssociatedArtifacts(),
                getContext(),
                getProjects(),
                getSubjects(),
                getAdditionalIdentifiers(),
                getPublicationNotes(),
                getDuplicateOf(),
                getStatus(),
                getImportDetails());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreatePublicationRequest that)) {
            return false;
        }
        return Objects.equals(getEntityDescription(), that.getEntityDescription())
                && Objects.equals(getAssociatedArtifacts(), that.getAssociatedArtifacts())
                && Objects.equals(getContext(), that.getContext())
                && Objects.equals(getProjects(), that.getProjects())
                && Objects.equals(getSubjects(), that.getSubjects())
                && Objects.equals(getDuplicateOf(), that.getDuplicateOf())
                && Objects.equals(getAdditionalIdentifiers(), that.getAdditionalIdentifiers())
                && Objects.equals(getPublicationNotes(), that.getPublicationNotes())
                && Objects.equals(getStatus(), that.getStatus())
                && Objects.equals(getImportDetails(), that.getImportDetails());
    }
}
