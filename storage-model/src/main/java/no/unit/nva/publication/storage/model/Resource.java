package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import lombok.Data;
import lombok.NonNull;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.identifiers.SortableIdentifier;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.TooManyFields"})
@Data
public class Resource implements WithIdentifier {

    public static final String TYPE = Resource.class.getSimpleName();

    private SortableIdentifier identifier;
    @NonNull
    private PublicationStatus status;
    @NonNull
    private String owner;
    @NonNull
    private Organization publisher;

    private Instant createdDate;

    private String title;
    private Instant modifiedDate;
    private Instant publishedDate;
    private URI link;
    private FileSet files;

    public Resource() {
    }

    public Resource(Builder builder) {
        setIdentifier(builder.identifier);
        setOwner(builder.owner);
        setStatus(builder.status);
        setPublisher(builder.publisher);
        setCreatedDate(builder.createdDate);
        setModifiedDate(builder.modifiedDate);
        setPublishedDate(builder.publishedDate);
        setTitle(builder.title);
        setFiles(builder.files);
        setLink(builder.link);
    }

    public static Resource emptyResource(String userIdentifier, URI organizationId) {
        return emptyResource(userIdentifier, organizationId, SortableIdentifier.next());
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



    public Builder copy() {
        return this.builder()
            .withOwner(getOwner())
            .withPublisher(getPublisher())
            .withCreatedDate(getCreatedDate())
            .withStatus(getStatus())
            .withTitle(getTitle())
            .withIdentifier(getIdentifier())
            .withModifiedDate(getModifiedDate())
            .withPublishedDate(getPublishedDate())
            .withFiles(getFiles())
            .withLink(getLink());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        public URI link;
        private SortableIdentifier identifier;
        private PublicationStatus status;
        private String owner;
        private Organization publisher;
        private Instant createdDate;
        private Instant modifiedDate;
        private String title;
        private Instant publishedDate;
        private FileSet files;

        private Builder() {
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withStatus(PublicationStatus status) {
            this.status = status;
            return this;
        }

        public Builder withOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder withPublisher(Organization publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder withCreatedDate(Instant createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Resource build() {
            return new Resource(this);
        }

        public Builder withPublishedDate(Instant publicationDate) {
            this.publishedDate = publicationDate;
            return this;
        }

        public Builder withFiles(FileSet files) {
            this.files = files;
            return this;
        }

        public Builder withLink(URI link) {
            this.link = link;
            return this;
        }
    }
}

