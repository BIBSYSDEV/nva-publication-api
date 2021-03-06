package no.unit.nva.publication.storage.model;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
public class Resource implements WithIdentifier, RowLevelSecurity, WithStatus, ResourceUpdate {

    public static final String TYPE = Resource.class.getSimpleName();

    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty
    private PublicationStatus status;
    @JsonProperty
    private String owner;
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

    public Resource() {

    }

    public static Resource resourceQueryObject(UserInstance userInstance, SortableIdentifier resourceIdentifier) {
        return emptyResource(userInstance.getUserIdentifier(), userInstance.getOrganizationUri(),
                             resourceIdentifier);
    }

    public static Resource resourceQueryObject(SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }

    public static Resource emptyResource(String userIdentifier,
                                         URI organizationId,
                                         SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setPublisher(new Organization.Builder().withId(organizationId).build());
        resource.setOwner(userIdentifier);
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }

    public static Resource fromPublication(Publication publication) {
        return Resource.builder()
                   .withIdentifier(publication.getIdentifier())
                   .withOwner(publication.getOwner())
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
                   .build();
    }

    @JsonIgnore
    public static String getType() {
        return TYPE;
    }

    public static ResourceBuilder builder() {
        return new ResourceBuilder();
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
        return projects;
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
                   .withOwner(getOwner())
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
                   .withHandle(getHandle());
    }

    @Override
    public Publication toPublication() {
        return new Publication.Builder()
                   .withIdentifier(getIdentifier())
                   .withOwner(getOwner())
                   .withStatus(this.getStatus())
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
                   .build();
    }

    @Override
    @JsonIgnore
    public URI getCustomerId() {
        return nonNull(this.getPublisher()) ? this.getPublisher().getId() : null;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getOwner(), getPublisher(), getCreatedDate(),
                            getModifiedDate(),
                            getPublishedDate(), getIndexedDate(), getLink(), getFileSet(), getProjects(),
                            getEntityDescription(), getDoi(), getHandle());
    }

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
               && Objects.equals(getOwner(), resource.getOwner())
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
               && Objects.equals(getHandle(), resource.getHandle());
    }
}

