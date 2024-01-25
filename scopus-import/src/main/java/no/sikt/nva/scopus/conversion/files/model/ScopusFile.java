package no.sikt.nva.scopus.conversion.files.model;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.File;

public record ScopusFile(UUID identifier, String name, URI downloadFileUrl, InputStream content, long size,
                         String mimeType, URI license, boolean publisherAuthority, Instant embargo) {

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder()
                   .withPublisherAuthority(this.publisherAuthority)
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
                   .withLicense(license)
                   .withPublisherAuthority(publisherAuthority)
                   .withEmbargoDate(embargo)
                   .buildPublishedFile();
    }

    public static final class Builder {

        private UUID identifier;
        private String name;
        private URI downloadFileUrl;
        private InputStream content;
        private long size;
        private String contentType;
        private URI license;
        private boolean publisherAuthority;
        private Instant embargo;

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

        public Builder withContent(InputStream content) {
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

        public Builder withPublisherAuthority(boolean publisherAuthority) {
            this.publisherAuthority = publisherAuthority;
            return this;
        }

        public Builder withEmbargo(Instant embargo) {
            this.embargo = embargo;
            return this;
        }

        public ScopusFile build() {
            return new ScopusFile(identifier, name, downloadFileUrl, content, size, contentType, license,
                                  publisherAuthority, embargo);
        }
    }
}
