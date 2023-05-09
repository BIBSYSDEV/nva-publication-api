package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingList;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.GodClass", "PMD.TooManyFields", "PMD.ExcessivePublicCount"})
@JsonTypeInfo(use = Id.NAME, property = "type")
public class Resource implements Entity {

    public static final String TYPE = "Resource";
    public static final URI NOT_IMPORTANT = null;

    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty
    private PublicationStatus status;
    @JsonProperty
    private Owner resourceOwner;
    @JsonProperty
    private Organization publisher;
    @JsonProperty
    private Instant createdDate;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    private Instant publishedDate;
    @JsonProperty
    private Instant indexedDate;
    @JsonProperty
    private URI link;
    @JsonProperty
    private AssociatedArtifactList associatedArtifacts;

    @JsonProperty
    private List<ResearchProject> projects;
    @JsonProperty
    private EntityDescription entityDescription;
    @JsonProperty
    private URI doi;
    @JsonProperty
    private URI handle;
    @JsonProperty
    private Set<AdditionalIdentifier> additionalIdentifiers;
    @JsonProperty
    private List<URI> subjects;
    @JsonProperty
    private List<Funding> fundings;
    @JsonProperty
    private String rightsHolder;
    @JsonProperty
    private ImportStatus importStatus;

    public static Resource resourceQueryObject(UserInstance userInstance, SortableIdentifier resourceIdentifier) {
        return emptyResource(userInstance.getUser(), userInstance.getOrganizationUri(),
                             resourceIdentifier);
    }

    public static Resource resourceQueryObject(SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }

    public static Resource fetchForElevatedUserQueryObject(URI customerId, SortableIdentifier resourceIdentifier) {
        return Resource.builder().withIdentifier(resourceIdentifier)
                   .withPublisher(new Organization.Builder().withId(customerId).build())
                   .build();
    }

    public static Resource emptyResource(User username,
                                         URI organizationId,
                                         SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setPublisher(new Organization.Builder().withId(organizationId).build());
        resource.setResourceOwner(new Owner(username, NOT_IMPORTANT));
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }

    public static Resource fromPublication(Publication publication) {
        return Optional.ofNullable(publication).map(Resource::convertToResource).orElse(null);
    }

    private static Resource convertToResource(Publication publication) {
        return Resource.builder()
                   .withIdentifier(publication.getIdentifier())
                   .withResourceOwner(Owner.fromResourceOwner(publication.getResourceOwner()))
                   .withCreatedDate(publication.getCreatedDate())
                   .withModifiedDate(publication.getModifiedDate())
                   .withIndexedDate(publication.getIndexedDate())
                   .withPublishedDate(publication.getPublishedDate())
                   .withStatus(publication.getStatus())
                   .withPublishedDate(publication.getPublishedDate())
                   .withAssociatedArtifactsList(publication.getAssociatedArtifacts())
                   .withPublisher(publication.getPublisher())
                   .withLink(publication.getLink())
                   .withProjects(publication.getProjects())
                   .withEntityDescription(publication.getEntityDescription())
                   .withDoi(publication.getDoi())
                   .withHandle(publication.getHandle())
                   .withAdditionalIdentifiers(publication.getAdditionalIdentifiers())
                   .withSubjects(publication.getSubjects())
                   .withFundings(publication.getFundings())
                   .withRightsHolder(publication.getRightsHolder())
                   .build();
    }

    private static Resource convertToResource(ImportCandidate importCandidate) {
        return Resource.builder()
                .withIdentifier(importCandidate.getIdentifier())
                .withResourceOwner(Owner.fromResourceOwner(importCandidate.getResourceOwner()))
                .withCreatedDate(importCandidate.getCreatedDate())
                .withModifiedDate(importCandidate.getModifiedDate())
                .withIndexedDate(importCandidate.getIndexedDate())
                .withPublishedDate(importCandidate.getPublishedDate())
                .withStatus(importCandidate.getStatus())
                .withPublishedDate(importCandidate.getPublishedDate())
                .withAssociatedArtifactsList(importCandidate.getAssociatedArtifacts())
                .withPublisher(importCandidate.getPublisher())
                .withLink(importCandidate.getLink())
                .withProjects(importCandidate.getProjects())
                .withEntityDescription(importCandidate.getEntityDescription())
                .withDoi(importCandidate.getDoi())
                .withHandle(importCandidate.getHandle())
                .withAdditionalIdentifiers(importCandidate.getAdditionalIdentifiers())
                .withSubjects(importCandidate.getSubjects())
                .withFundings(importCandidate.getFundings())
                .withRightsHolder(importCandidate.getRightsHolder())
                .withImportStatus(ImportStatus.NOT_IMPORTED)
                .build();
    }

    public static ResourceBuilder builder() {
        return new ResourceBuilder();
    }

    public static Resource fromImportCandidate(ImportCandidate importCandidate) {
        return Optional.ofNullable(importCandidate).map(Resource::convertToResource).orElse(null);

    }

    public Publication persistNew(ResourceService resourceService, UserInstance userInstance)
        throws BadRequestException {
        return resourceService.createPublication(userInstance, this.toPublication());
    }

    public Owner getResourceOwner() {
        return resourceOwner;
    }

    public void setResourceOwner(Owner resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    public Set<AdditionalIdentifier> getAdditionalIdentifiers() {
        return nonNull(additionalIdentifiers) ? additionalIdentifiers : Collections.emptySet();
    }

    public void setAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public ImportStatus getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(ImportStatus importStatus) {
        this.importStatus = importStatus;
    }

    @Override
    public Publication toPublication(ResourceService resourceService) {
        return toPublication();
    }

    public Publication toPublication() {
        return new Publication.Builder()
                   .withIdentifier(getIdentifier())
                   .withResourceOwner(extractResourceOwner())
                   .withStatus(getStatus())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withIndexedDate(getIndexedDate())
                   .withPublisher(getPublisher())
                   .withPublishedDate(getPublishedDate())
                   .withLink(getLink())
                   .withProjects(getProjects())
                   .withEntityDescription(getEntityDescription())
                   .withDoi(getDoi())
                   .withHandle(getHandle())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withAssociatedArtifacts(getAssociatedArtifacts())
                   .withSubjects(getSubjects())
                   .withFundings(getFundings())
                   .withRightsHolder(getRightsHolder())
                   .build();
    }

    public ImportCandidate toImportCandidate() {
        return new ImportCandidate.Builder()
                .withIdentifier(getIdentifier())
                .withResourceOwner(extractResourceOwner())
                .withStatus(getStatus())
                .withCreatedDate(getCreatedDate())
                .withModifiedDate(getModifiedDate())
                .withIndexedDate(getIndexedDate())
                .withPublisher(getPublisher())
                .withPublishedDate(getPublishedDate())
                .withLink(getLink())
                .withProjects(getProjects())
                .withEntityDescription(getEntityDescription())
                .withDoi(getDoi())
                .withHandle(getHandle())
                .withAdditionalIdentifiers(getAdditionalIdentifiers())
                .withAssociatedArtifacts(getAssociatedArtifacts())
                .withSubjects(getSubjects())
                .withFundings(getFundings())
                .withRightsHolder(getRightsHolder())
                .withImportStatus(getImportStatus())
                .build();
    }

    private ResourceOwner extractResourceOwner() {
        return Optional.ofNullable(getResourceOwner()).map(Owner::toResourceOwner).orElse(null);
    }

    @Override
    public String getType() {
        return TYPE;
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
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @JsonIgnore
    @Override
    public User getOwner() {
        return getResourceOwner().getUser();
    }

    @Override
    @JsonIgnore
    public URI getCustomerId() {
        return nonNull(this.getPublisher()) ? this.getPublisher().getId() : null;
    }

    @Override
    public Dao toDao() {
        return new ResourceDao(this);
    }

    @Override
    public String getStatusString() {
        return nonNull(getStatus()) ? getStatus().toString() : null;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public Organization getPublisher() {
        return publisher;
    }

    public void setPublisher(Organization publisher) {
        this.publisher = publisher;
    }

    public Instant getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
    }

    public Instant getIndexedDate() {
        return indexedDate;
    }

    public void setIndexedDate(Instant indexedDate) {
        this.indexedDate = indexedDate;
    }

    public URI getLink() {
        return link;
    }

    public void setLink(URI link) {
        this.link = link;
    }

    public AssociatedArtifactList getAssociatedArtifacts() {
        return associatedArtifacts;
    }

    public void setAssociatedArtifacts(AssociatedArtifactList associatedArtifacts) {
        this.associatedArtifacts = associatedArtifacts;
    }

    public List<ResearchProject> getProjects() {
        return nonNull(projects) ? projects : Collections.emptyList();
    }

    public void setProjects(List<ResearchProject> projects) {
        this.projects = projects;
    }

    public EntityDescription getEntityDescription() {
        return entityDescription;
    }

    public void setEntityDescription(EntityDescription entityDescription) {
        this.entityDescription = entityDescription;
    }

    public URI getDoi() {
        return doi;
    }

    public void setDoi(URI doi) {
        this.doi = doi;
    }

    public URI getHandle() {
        return handle;
    }

    public void setHandle(URI handle) {
        this.handle = handle;
    }

    public ResourceBuilder copy() {
        return Resource.builder()
                   .withIdentifier(getIdentifier())
                   .withStatus(getStatus())
                   .withResourceOwner(Owner.fromResourceOwner(extractResourceOwner()))
                   .withPublisher(getPublisher())
                   .withCreatedDate(getCreatedDate())
                   .withModifiedDate(getModifiedDate())
                   .withPublishedDate(getPublishedDate())
                   .withIndexedDate(getIndexedDate())
                   .withLink(getLink())
                   .withAssociatedArtifactsList(getAssociatedArtifacts())
                   .withProjects(getProjects())
                   .withEntityDescription(getEntityDescription())
                   .withDoi(getDoi())
                   .withHandle(getHandle())
                   .withAdditionalIdentifiers(getAdditionalIdentifiers())
                   .withSubjects(getSubjects())
                   .withFundings(getFundings())
                   .withRightsHolder(getRightsHolder());
    }

    public List<URI> getSubjects() {
        return nonNull(subjects) ? subjects : Collections.emptyList();
    }

    public void setSubjects(List<URI> subjects) {
        this.subjects = subjects;
    }

    public List<Funding> getFundings() {
        return nonNull(fundings) ? fundings : Collections.emptyList();
    }

    public void setFundings(List<Funding> fundings) {
        this.fundings = new FundingList(fundings);
    }

    public String getRightsHolder() {
        return rightsHolder;
    }

    public void setRightsHolder(String rightsHolder) {
        this.rightsHolder = rightsHolder;
    }

    /**
     * Calculates hashcode without considering the row version.
     *
     * @return the hashcode.
     */
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getResourceOwner(), getPublisher(), getCreatedDate(),
                            getModifiedDate(), getPublishedDate(), getIndexedDate(), getLink(),
                            getProjects(), getEntityDescription(), getDoi(), getHandle(), getAdditionalIdentifiers(),
                            getSubjects(), getFundings(), getAssociatedArtifacts());
    }

    /**
     * It compares two Resources ignoring the row version.
     *
     * @param o the other Resource.
     * @return true if the two Resources are equivalent without considering the row version, false otherwise.
     */
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource resource = (Resource) o;
        return Objects.equals(getIdentifier(), resource.getIdentifier())
               && getStatus() == resource.getStatus()
               && Objects.equals(getResourceOwner(), resource.getResourceOwner())
               && Objects.equals(getPublisher(), resource.getPublisher())
               && Objects.equals(getCreatedDate(), resource.getCreatedDate())
               && Objects.equals(getModifiedDate(), resource.getModifiedDate())
               && Objects.equals(getPublishedDate(), resource.getPublishedDate())
               && Objects.equals(getIndexedDate(), resource.getIndexedDate())
               && Objects.equals(getLink(), resource.getLink())
               && Objects.equals(getProjects(), resource.getProjects())
               && Objects.equals(getEntityDescription(), resource.getEntityDescription())
               && Objects.equals(getDoi(), resource.getDoi())
               && Objects.equals(getHandle(), resource.getHandle())
               && Objects.equals(getAdditionalIdentifiers(), resource.getAdditionalIdentifiers())
               && Objects.equals(getAssociatedArtifacts(), resource.getAssociatedArtifacts())
               && Objects.equals(getFundings(), resource.getFundings())
               && Objects.equals(getSubjects(), resource.getSubjects());
    }

    public Stream<TicketEntry> fetchAllTickets(ResourceService resourceService) {
        return resourceService.fetchAllTicketsForResource(this);
    }
}

