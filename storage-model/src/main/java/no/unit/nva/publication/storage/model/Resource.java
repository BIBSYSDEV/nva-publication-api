package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import lombok.Data;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.identifiers.SortableIdentifier;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields"})

@Data
@lombok.Builder(
    builderClassName = "ResourceBuilder",
    builderMethodName = "builder",
    toBuilder = true,
    setterPrefix = "with")
public class Resource implements WithIdentifier {

    public static final String TYPE = Resource.class.getSimpleName();

    private SortableIdentifier identifier;

    private PublicationStatus status;

    private String owner;

    private Organization publisher;

    private Instant createdDate;

    private String title;
    private Instant modifiedDate;
    private Instant publishedDate;
    private URI link;
    private FileSet files;

    //for lombok.
    @SuppressWarnings("PMD.ExcessiveParameterList")
    private Resource(SortableIdentifier identifier,
                    PublicationStatus status,
                    String owner,
                    Organization publisher,
                    Instant createdDate,
                    String title,
                    Instant modifiedDate,
                    Instant publishedDate,
                    URI link,
                    FileSet files) {
        this.identifier = identifier;
        this.status = status;
        this.owner = owner;
        this.publisher = publisher;
        this.createdDate = createdDate;
        this.title = title;
        this.modifiedDate = modifiedDate;
        this.publishedDate = publishedDate;
        this.link = link;
        this.files = files;
    }

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
}

