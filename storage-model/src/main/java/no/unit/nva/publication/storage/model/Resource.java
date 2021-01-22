package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResearchProject;

//TODO: Remove all Lombok dependencies from the final class.

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields"})
@Data
@Builder(
    builderClassName = "ResourceBuilder",
    builderMethodName = "builder",
    toBuilder = true,
    setterPrefix = "with")
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class Resource implements WithIdentifier {

    public static final String TYPE = Resource.class.getSimpleName();

    private SortableIdentifier identifier;
    private PublicationStatus status;
    private String owner;
    private Organization publisher;
    private Instant createdDate;
    private Instant modifiedDate;
    private Instant publishedDate;
    private Instant indexedDate;
    private URI link;
    private FileSet fileSet;
    private List<ResearchProject> projects;
    private EntityDescription entityDescription;
    private URI doi;
    private URI handle;

    public Resource() {

    }

    public static Resource emptyResource(String userIdentifier, URI organizationId,
                                         String resourceIdentifier) {
        return emptyResource(userIdentifier, organizationId, new SortableIdentifier(resourceIdentifier));
    }

    public static Resource emptyResource(String userIdentifier, URI organizationId,
                                         SortableIdentifier resourceIdentifier) {
        Resource resource = new Resource();
        resource.setPublisher(new Organization.Builder().withId(organizationId).build());
        resource.setOwner(userIdentifier);
        resource.setIdentifier(resourceIdentifier);
        return resource;
    }

    public ResourceBuilder copy() {
        return this.toBuilder();
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

    public Publication toPublication() {
        return new Publication.Builder()
            .withIdentifier(getIdentifier())
            .withOwner(getOwner())
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
            .withDoiRequest(emptyDoiRequest())
            .withDoi(getDoi())
            .withHandle(getHandle())
            .build();
    }



    private DoiRequest emptyDoiRequest() {
        return new DoiRequest.Builder().build();
    }
}

