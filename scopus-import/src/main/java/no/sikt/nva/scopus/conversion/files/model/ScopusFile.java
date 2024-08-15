package no.sikt.nva.scopus.conversion.files.model;

import static no.sikt.nva.scopus.ScopusConstants.UPLOAD_DETAILS_USERNAME;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails.Source;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import org.apache.tika.io.TikaInputStream;

public record ScopusFile(UUID identifier, String name, URI downloadFileUrl, TikaInputStream content, long size,
                         String mimeType, URI license, PublisherVersion publisherVersion, Instant embargo) {

    private static final List<String> UNSUPPORTED_MIME_TYPES = List.of("text/html", "application/octet-stream");

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder()
                   .withPublisherVersion(this.publisherVersion)
                   .withEmbargo(this.embargo)
                   .withLicense(this.license)
                   .withName(this.name)
                   .withDownloadFileUrl(this.downloadFileUrl)
                   .withIdentifier(this.identifier)
                   .withSize(this.size)
                   .withContent(this.content)
                   .withMimeType(this.mimeType);
    }

    public AssociatedArtifact toPublishedAssociatedArtifact() {
        return File.builder()
                   .withIdentifier(identifier)
                   .withName(name)
                   .withMimeType(mimeType)
                   .withSize(size)
                   .withUploadDetails(createUploadDetails())
                   .withLicense(license)
                   .withPublisherVersion(publisherVersion)
                   .withEmbargoDate(embargo)
                   .buildPublishedFile();
    }

    private ImportUploadDetails createUploadDetails() {
        return new ImportUploadDetails(Source.SCOPUS, null, Instant.now());
    }

    public boolean hasValidMimeType() {
        return Optional.ofNullable(mimeType)
                   .map(mimeType -> !UNSUPPORTED_MIME_TYPES.contains(mimeType))
                   .orElse(false);
    }

    public static final class Builder {

        private UUID identifier;
        private String name;
        private URI downloadFileUrl;
        private TikaInputStream content;
        private long size;
        private String contentType;
        private URI license;
        private Instant embargo;
        private PublisherVersion publisherVersion;

        private Builder() {
        }

        public Builder withIdentifier(UUID identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDownloadFileUrl(URI downloadFileUrl) {
            this.downloadFileUrl = downloadFileUrl;
            return this;
        }

        public Builder withContent(TikaInputStream content) {
            this.content = content;
            return this;
        }

        public Builder withSize(long size) {
            this.size = size;
            return this;
        }

        public Builder withMimeType(String contentType) {
            this.contentType = contentType;
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

        public Builder withEmbargo(Instant embargo) {
            this.embargo = embargo;
            return this;
        }

        public ScopusFile build() {
            return new ScopusFile(identifier, name, downloadFileUrl, content, size, contentType, license,
                                  publisherVersion, embargo);
        }
    }
}
