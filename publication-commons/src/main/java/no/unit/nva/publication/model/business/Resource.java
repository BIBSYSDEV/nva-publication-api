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
import java.util.Set;
import java.util.UUID;
import no.unit.nva.file.model.FileSet;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.ResourceDao;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.GodClass", "PMD.TooManyFields", "PMD.ExcessivePublicCount"})
@JsonTypeInfo(use = Id.NAME, property = "type")
public class Resource implements WithIdentifier, RowLevelSecurity, WithStatus, DataEntry {
    
    public static final String TYPE = "Resource";
    public static final URI NOT_IMPORTANT = null;
    
    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty
    private PublicationStatus status;
    @JsonProperty
    private ResourceOwner resourceOwner;
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
    private FileSet fileSet;
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
    @JsonProperty(ROW_VERSION)
    private String rowVersion;
    
    public static Resource resourceQueryObject(UserInstance userInstance, SortableIdentifier resourceIdentifier) {
        return emptyResource(userInstance.getUserIdentifier(), userInstance.getOrganizationUri(),
            resourceIdentifier);
    }
    
    public static Resource resourceQueryObject(SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }
    
    public static Resource emptyResource(String username,
                                         URI organizationId,
                                         SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setPublisher(new Organization.Builder().withId(organizationId).build());
        resource.setResourceOwner(new ResourceOwner(username, NOT_IMPORTANT));
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }
    
    public static Resource fromPublication(Publication publication) {
        return Resource.builder()
            .withIdentifier(publication.getIdentifier())
            .withResourceOwner(publication.getResourceOwner())
            .withCreatedDate(publication.getCreatedDate())
            .withModifiedDate(publication.getModifiedDate())
            .withIndexedDate(publication.getIndexedDate())
            .withPublishedDate(publication.getPublishedDate())
            .withStatus(publication.getStatus())
            .withPublishedDate(publication.getPublishedDate())
            .withFileSet(publication.getFileSet())
            .withPublisher(publication.getPublisher())
            .withLink(publication.getLink())
            .withProjects(publication.getProjects())
            .withEntityDescription(publication.getEntityDescription())
            .withDoi(publication.getDoi())
            .withHandle(publication.getHandle())
            .withAdditionalIdentifiers(publication.getAdditionalIdentifiers())
            .withSubjects(publication.getSubjects())
            .withRowVersion(nextRowVersion())
            .build();
    }
    
    @JsonIgnore
    public static String getType() {
        return TYPE;
    }
    
    public static ResourceBuilder builder() {
        return new ResourceBuilder();
    }
    
    public static String nextRowVersion() {
        return UUID.randomUUID().toString();
    }
    
    public ResourceOwner getResourceOwner() {
        return resourceOwner;
    }
    
    public void setResourceOwner(ResourceOwner resourceOwner) {
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
    
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    public Instant getModifiedDate() {
        return modifiedDate;
    }
    
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
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
    
    public FileSet getFileSet() {
        return fileSet;
    }
    
    public void setFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
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
    
    @Override
    public String getStatusString() {
        return nonNull(getStatus()) ? getStatus().toString() : null;
    }
    
    public ResourceBuilder copy() {
        return Resource.builder()
            .withIdentifier(getIdentifier())
            .withStatus(getStatus())
            .withResourceOwner(getResourceOwner())
            .withResourceOwner(getResourceOwner())
            .withPublisher(getPublisher())
            .withCreatedDate(getCreatedDate())
            .withModifiedDate(getModifiedDate())
            .withPublishedDate(getPublishedDate())
            .withIndexedDate(getIndexedDate())
            .withLink(getLink())
            .withFileSet(getFileSet())
            .withProjects(getProjects())
            .withEntityDescription(getEntityDescription())
            .withDoi(getDoi())
            .withHandle(getHandle())
            .withAdditionalIdentifiers(getAdditionalIdentifiers())
            .withSubjects(getSubjects())
            .withRowVersion(getRowVersion());
    }
    
    @Override
    public Publication toPublication() {
        return new Publication.Builder()
            .withIdentifier(getIdentifier())
            .withResourceOwner(getResourceOwner())
            .withStatus(getStatus())
            .withCreatedDate(getCreatedDate())
            .withModifiedDate(getModifiedDate())
            .withIndexedDate(getIndexedDate())
            .withPublisher(getPublisher())
            .withPublishedDate(getPublishedDate())
            .withLink(getLink())
            .withFileSet(getFileSet())
            .withProjects(getProjects())
            .withEntityDescription(getEntityDescription())
            .withDoiRequest(null)
            .withDoi(getDoi())
            .withHandle(getHandle())
            .withAdditionalIdentifiers(getAdditionalIdentifiers())
            .withSubjects(getSubjects())
            .build();
    }
    
    @Override
    @JacocoGenerated
    public String getRowVersion() {
        return rowVersion;
    }
    
    @Override
    @JacocoGenerated
    public void setRowVersion(String rowVersion) {
        this.rowVersion = rowVersion;
    }
    
    @Override
    public Dao<?> toDao() {
        return new ResourceDao(this);
    }
    
    @Override
    @JsonIgnore
    public URI getCustomerId() {
        return nonNull(this.getPublisher()) ? this.getPublisher().getId() : null;
    }
    
    @JsonIgnore
    @Override
    public String getOwner() {
        return getResourceOwner().getOwner();
    }
    
    public List<URI> getSubjects() {
        return nonNull(subjects) ? subjects : Collections.emptyList();
    }
    
    public void setSubjects(List<URI> subjects) {
        this.subjects = subjects;
    }
    
    /**
     * Calculates hashcode without considering the row version.
     *
     * @return the hashcode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getResourceOwner(), getPublisher(), getCreatedDate(),
            getModifiedDate(), getPublishedDate(), getIndexedDate(), getLink(), getFileSet(),
            getProjects(),
            getEntityDescription(), getDoi(), getHandle(), getAdditionalIdentifiers(), getSubjects());
    }
    
    /**
     * It compares two Resources ignoring the row version.
     *
     * @param o the other Resource.
     * @return true if the two Resources are equivalent without considering the row version, false otherwise.
     */
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
               && Objects.equals(getFileSet(), resource.getFileSet())
               && Objects.equals(getProjects(), resource.getProjects())
               && Objects.equals(getEntityDescription(), resource.getEntityDescription())
               && Objects.equals(getDoi(), resource.getDoi())
               && Objects.equals(getHandle(), resource.getHandle())
               && Objects.equals(getAdditionalIdentifiers(), resource.getAdditionalIdentifiers())
               && Objects.equals(getSubjects(), resource.getSubjects());
    }
}

