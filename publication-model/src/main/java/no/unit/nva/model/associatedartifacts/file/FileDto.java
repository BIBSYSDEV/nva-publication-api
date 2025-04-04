package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactDto;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;


public record FileDto(
    @JsonProperty(File.IDENTIFIER_FIELD) SortableIdentifier identifier,
    @JsonProperty(FileDto.TYPE_NAME_FIELD) String type,
    @JsonProperty(File.NAME_FIELD) String name,
    @JsonProperty(File.MIME_TYPE_FIELD) String mimeType,
    @JsonProperty(File.SIZE_FIELD) Long size,
    @JsonProperty(File.LICENSE_FIELD) URI license,
    @JsonProperty(File.PUBLISHER_VERSION_FIELD) PublisherVersion publisherVersion,
    @JsonProperty(File.EMBARGO_DATE_FIELD) Instant embargoDate,
    @JsonProperty(File.RIGHTS_RETENTION_STRATEGY) RightsRetentionStrategy rightsRetentionStrategy,
    @JsonProperty(File.LEGAL_NOTE_FIELD) String legalNote,
    @JsonProperty(File.PUBLISHED_DATE_FIELD) Instant publishedDate,
    @JsonProperty(File.UPLOAD_DETAILS_FIELD) UploadDetails uploadDetails,
    @JsonProperty(FileDto.ALLOWED_OPERATIONS_FIELD) Set<FileOperation> allowedOperations
) implements AssociatedArtifactDto {

    public static final String ALLOWED_OPERATIONS_FIELD = "allowedOperations";
    public static final String TYPE_NAME_FIELD = "type";

    @Override
    public String getArtifactType() {
        return type;
    }

    // Static Builder class
    public static class Builder {
        private SortableIdentifier identifier;
        private String type;
        private String name;
        private String mimeType;
        private Long size;
        private URI license;
        private PublisherVersion publisherVersion;
        private Instant embargoDate;
        private RightsRetentionStrategy rightsRetentionStrategy;
        private String legalNote;
        private Instant publishedDate;
        private UploadDetails uploadDetails;
        private Set<FileOperation> allowedOperations;

        // Builder methods to set each field
        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withType(String type) {
            this.type = type;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder withSize(Long size) {
            this.size = size;
            return this;
        }

        public Builder withLicense(URI license) {
            this.license = license;
            return this;
        }

        public Builder withPublisherVersion(PublisherVersion publisherVersion) {
            this.publisherVersion = publisherVersion;
            return this;
        }

        public Builder withEmbargoDate(Instant embargoDate) {
            this.embargoDate = embargoDate;
            return this;
        }

        public Builder withRightsRetentionStrategy(RightsRetentionStrategy rightsRetentionStrategy) {
            this.rightsRetentionStrategy = rightsRetentionStrategy;
            return this;
        }

        public Builder withLegalNote(String legalNote) {
            this.legalNote = legalNote;
            return this;
        }

        public Builder withPublishedDate(Instant publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public Builder withUploadDetails(UploadDetails uploadDetails) {
            this.uploadDetails = uploadDetails;
            return this;
        }

        public Builder withAllowedOperations(Set<FileOperation> allowedOperations) {
            this.allowedOperations = allowedOperations;
            return this;
        }

        public FileDto build() {
            return new FileDto(
                identifier,
                type,
                name,
                mimeType,
                size,
                license,
                publisherVersion,
                embargoDate,
                rightsRetentionStrategy,
                legalNote,
                publishedDate,
                uploadDetails,
                allowedOperations
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder()
                   .withIdentifier(this.identifier())
                   .withType(this.type())
                   .withName(this.name())
                   .withMimeType(this.mimeType())
                   .withSize(this.size())
                   .withLicense(this.license())
                   .withPublisherVersion(this.publisherVersion())
                   .withEmbargoDate(this.embargoDate())
                   .withRightsRetentionStrategy(this.rightsRetentionStrategy())
                   .withLegalNote(this.legalNote())
                   .withPublishedDate(this.publishedDate())
                   .withUploadDetails(this.uploadDetails())
                   .withAllowedOperations(this.allowedOperations());
    }
}