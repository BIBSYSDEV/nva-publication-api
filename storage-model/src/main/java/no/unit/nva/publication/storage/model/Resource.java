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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;

//TODO: Remove all Lombok dependencies from the final class.

@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields"})
@Data
@Builder(
    builderClassName = "ResourceBuilder",
    builderMethodName = "builder",
    toBuilder = true,
    setterPrefix = "with")
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class Resource implements WithIdentifier, RowLevelSecurity, WithStatus {

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

    public ResourceBuilder copy() {
        return this.toBuilder();
    }

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
        return this.getPublisher().getId();
    }

    @Override
    public String getStatusString() {
        return nonNull(getStatus()) ? getStatus().toString() : null;
    }
}

