package no.unit.nva.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.WithAdditionalIdentifiers;
import no.unit.nva.WithAssociatedArtifact;
import no.unit.nva.WithContext;
import no.unit.nva.WithId;
import no.unit.nva.WithIdentifier;
import no.unit.nva.WithInternal;
import no.unit.nva.WithMetadata;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifierBase;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.TooManyFields", "PMD.GodClass", "PMD.ExcessivePublicCount"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("Publication")
public class PublicationResponse implements WithIdentifier, WithInternal, WithMetadata, WithAssociatedArtifact, WithId,
                                            WithContext, WithAdditionalIdentifiers {

    private SortableIdentifier identifier;
    private PublicationStatus status;
    private ResourceOwner resourceOwner;
    private Organization publisher;
    private Instant createdDate;
    private Instant modifiedDate;
    private Instant publishedDate;
    private Instant indexedDate;
    private URI handle;
    private URI link;
    private EntityDescription entityDescription;
    private URI doi;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<Funding> fundings;
    private List<URI> subjects;
    private AssociatedArtifactList associatedArtifacts;
    private List<ImportDetail> importDetails;

    private Set<AdditionalIdentifierBase> additionalIdentifiers;
    private String rightsHolder;

    private Set<PublicationOperation> allowedOperations;

    private URI duplicateOf;

    public static PublicationResponse fromPublication(Publication publication) {
        var response = new PublicationResponse();
        response.setIdentifier(publication.getIdentifier());
        response.setStatus(publication.getStatus());
        response.setDuplicateOf(publication.getDuplicateOf());
        response.setResourceOwner(publication.getResourceOwner());
        response.setPublisher(publication.getPublisher());
        response.setCreatedDate(publication.getCreatedDate());
        response.setModifiedDate(publication.getModifiedDate());
        response.setPublishedDate(publication.getPublishedDate());
        response.setIndexedDate(publication.getIndexedDate());
        response.setHandle(publication.getHandle());
        response.setLink(publication.getLink());
        response.setEntityDescription(publication.getEntityDescription());
        response.setAssociatedArtifacts(publication.getAssociatedArtifacts());
        response.setDoi(publication.getDoi());
        response.setProjects(publication.getProjects());
        response.setFundings(publication.getFundings());
        response.setSubjects(publication.getSubjects());
        response.setContext(PublicationContext.getContext(publication));
        response.setAssociatedArtifacts(publication.getAssociatedArtifacts());
        response.setAdditionalIdentifiers(publication.getAdditionalIdentifiers());
        response.setRightsHolder(publication.getRightsHolder());
        response.setAllowedOperations(Set.of());
        response.setImportDetails(publication.getImportDetails());
        return response;
    }

    public static PublicationResponse fromPublicationWithAllowedOperations(
        Publication publication,
        Set<PublicationOperation> allowedOperations
    ) {
        var response = fromPublication(publication);
        response.setAllowedOperations(allowedOperations);
        return response;
    }

    public void setDuplicateOf(URI duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public URI getDuplicateOf() {
        return duplicateOf;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public PublicationStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    @Override
    public URI getHandle() {
        return handle;
    }

    @Override
    public void setHandle(URI handle) {
        this.handle = handle;
    }

    @Override
    public Instant getPublishedDate() {
        return publishedDate;
    }

    @Override
    public void setPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public ResourceOwner getResourceOwner() {
        return resourceOwner;
    }

    @Override
    public void setResourceOwner(ResourceOwner resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    @Override
    public Instant getIndexedDate() {
        return indexedDate;
    }

    @Override
    public void setIndexedDate(Instant indexedDate) {
        this.indexedDate = indexedDate;
    }

    @Override
    public URI getLink() {
        return link;
    }

    @Override
    public void setLink(URI link) {
        this.link = link;
    }

    @Override
    public Organization getPublisher() {
        return publisher;
    }

    @Override
    public void setPublisher(Organization publisher) {
        this.publisher = publisher;
    }

    @Override
    public URI getDoi() {
        return doi;
    }

    @Override
    public void setDoi(URI doi) {
        this.doi = doi;
    }

    @Override
    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    @Override
    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    @Override
    public List<ResearchProject> getProjects() {
        return Objects.nonNull(projects) ? projects : Collections.emptyList();
    }

    @Override
    public void setProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }

    @Override
    public List<URI> getSubjects() {
        return Objects.nonNull(subjects) ? subjects : Collections.emptyList();
    }

    @Override
    public void setSubjects(List<URI> subjects) {
        this.subjects = subjects;
    }

    @Override
    public List<Funding> getFundings() {
        return fundings;
    }

    @Override
    public void setFundings(List<Funding> fundings) {
        this.fundings = fundings;
    }

    @Override
    public String getRightsHolder() {
        return rightsHolder;
    }

    @Override
    public void setRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
    }

    @Override
    public JsonNode getContext() {
        return context;
    }

    @Override
    public void setContext(JsonNode context) {
        this.context = context;
    }

    @Override
    public AssociatedArtifactList getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    @Override
    public void setAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    @Override
    public List<ImportDetail> getImportDetails() {
        return importDetails;
    }

    @Override
    public void setImportDetails(Collection<ImportDetail> importDetails) {
        this.importDetails = new ArrayList<>(importDetails);
    }

    @Override
    public Set<AdditionalIdentifierBase> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    @Override
    public void setAdditionalIdentifiers(Set<AdditionalIdentifierBase> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    public Set<PublicationOperation> getAllowedOperations() {
        return allowedOperations;
    }

    public void setAllowedOperations(Set<PublicationOperation> allowedOperations) {
        this.allowedOperations = allowedOperations;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(),
                            getStatus(),
                            getDuplicateOf(),
                            getResourceOwner(),
                            getPublisher(),
                            getCreatedDate(),
                            getModifiedDate(),
                            getPublishedDate(),
                            getIndexedDate(),
                            getHandle(),
                            getLink(),
                            getEntityDescription(),
                            getDoi(),
                            getContext(),
                            getProjects(),
                            getFundings(),
                            getSubjects(),
                            getAdditionalIdentifiers(),
                            getAssociatedArtifacts(),
                            getRightsHolder(),
                            getAllowedOperations(),
                            getImportDetails());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicationResponse that)) {
            return false;
        }
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && getStatus() == that.getStatus()
               && Objects.equals(getDuplicateOf(), that.getDuplicateOf())
               && Objects.equals(getResourceOwner(), that.getResourceOwner())
               && Objects.equals(getPublisher(), that.getPublisher())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getPublishedDate(), that.getPublishedDate())
               && Objects.equals(getIndexedDate(), that.getIndexedDate())
               && Objects.equals(getHandle(), that.getHandle())
               && Objects.equals(getLink(), that.getLink())
               && Objects.equals(getEntityDescription(), that.getEntityDescription())
               && Objects.equals(getDoi(), that.getDoi())
               && Objects.equals(getContext(), that.getContext())
               && Objects.equals(getProjects(), that.getProjects())
               && Objects.equals(getFundings(), that.getFundings())
               && Objects.equals(getSubjects(), that.getSubjects())
               && Objects.equals(getAdditionalIdentifiers(), that.getAdditionalIdentifiers())
               && Objects.equals(getAssociatedArtifacts(), that.getAssociatedArtifacts())
               && Objects.equals(getRightsHolder(), that.getRightsHolder())
               && Objects.equals(getAllowedOperations(), that.getAllowedOperations())
               && Objects.equals(getImportDetails(), that.getImportDetails());
    }
}
