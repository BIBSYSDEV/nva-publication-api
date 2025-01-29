package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactResponse;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;

public record FileResponse(
    @JsonProperty(File.IDENTIFIER_FIELD) UUID identifier,
    @JsonProperty(FileResponse.TYPE_NAME_FIELD) String type,
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
    @JsonProperty(FileResponse.ALLOWED_OPERATIONS_FIELD) Set<FileOperation> allowedOperations
) implements AssociatedArtifactResponse {

    public static final String ALLOWED_OPERATIONS_FIELD = "allowedOperations";
    public static final String TYPE_NAME_FIELD = "type";

    @Override
    public String getType() {
        return type;
    }

    // Static Builder class
    public static class Builder {
        private UUID identifier;
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
        public Builder identifier(UUID identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder size(Long size) {
            this.size = size;
            return this;
        }

        public Builder license(URI license) {
            this.license = license;
            return this;
        }

        public Builder publisherVersion(PublisherVersion publisherVersion) {
            this.publisherVersion = publisherVersion;
            return this;
        }

        public Builder embargoDate(Instant embargoDate) {
            this.embargoDate = embargoDate;
            return this;
        }

        public Builder rightsRetentionStrategy(RightsRetentionStrategy rightsRetentionStrategy) {
            this.rightsRetentionStrategy = rightsRetentionStrategy;
            return this;
        }

        public Builder legalNote(String legalNote) {
            this.legalNote = legalNote;
            return this;
        }

        public Builder publishedDate(Instant publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public Builder uploadDetails(UploadDetails uploadDetails) {
            this.uploadDetails = uploadDetails;
            return this;
        }

        public Builder allowedOperations(Set<FileOperation> allowedOperations) {
            this.allowedOperations = allowedOperations;
            return this;
        }

        // Build method to create a new FileResponse instance
        public FileResponse build() {
            return new FileResponse(
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

    // Static method to create a builder from an existing instance
    public Builder copy() {
        return new Builder()
                   .identifier(this.identifier())
                   .type(this.type())
                   .name(this.name())
                   .mimeType(this.mimeType())
                   .size(this.size())
                   .license(this.license())
                   .publisherVersion(this.publisherVersion())
                   .embargoDate(this.embargoDate())
                   .rightsRetentionStrategy(this.rightsRetentionStrategy())
                   .legalNote(this.legalNote())
                   .publishedDate(this.publishedDate())
                   .uploadDetails(this.uploadDetails())
                   .allowedOperations(this.allowedOperations());
    }
}