package no.unit.nva.publication.download;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(PresignedUrl.TYPE)
public record PresignedUrl(UUID fileIdentifier, @JsonIgnore String bucket, URI uri, Instant expires) {

    public static final String TYPE = "PresignedUrl";

    public static PresignedUrl fromS3Key(UUID fileIdentifier) {
        return PresignedUrl.builder().withFileIdentifier(fileIdentifier).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public PresignedUrl bucket(String bucket) {
        return this.copy().withBucket(bucket).build();
    }

    public Builder copy() {
        return builder().withFileIdentifier(this.fileIdentifier)
                   .withBucket(this.bucket)
                   .withUri(this.uri)
                   .withExpiration(this.expires);
    }

    public PresignedUrl fromS3PresignerResponse(PresignedGetObjectRequest response) {
        return PresignedUrl.builder()
                   .withFileIdentifier(fileIdentifier)
                   .withUri(attempt(() -> response.url().toURI()).orElseThrow())
                   .withExpiration(response.expiration())
                   .build();
    }

    public PresignedUrl create(S3Presigner s3Presigner) {
        return attempt(() -> getS3ObjectRequest(fileIdentifier)).map(this::getPresignedUrlRequest)
                   .map(s3Presigner::presignGetObject)
                   .map(this::fromS3PresignerResponse)
                   .orElseThrow();
    }

    private GetObjectPresignRequest getPresignedUrlRequest(GetObjectRequest getObjectReqeust) {
        return GetObjectPresignRequest.builder()
                   .signatureDuration(Duration.ofMinutes(10))
                   .getObjectRequest(getObjectReqeust)
                   .build();
    }

    private GetObjectRequest getS3ObjectRequest(UUID fileIdentifier) {
        return GetObjectRequest.builder().bucket(bucket).key(fileIdentifier.toString()).build();
    }

    public static final class Builder {

        private UUID fileIdentifier;
        private URI uri;
        private Instant expiration;
        private String bucket;

        private Builder() {
        }

        public Builder withFileIdentifier(UUID fileIdentifier) {
            this.fileIdentifier = fileIdentifier;
            return this;
        }

        public Builder withBucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public Builder withUri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Builder withExpiration(Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        public PresignedUrl build() {
            return new PresignedUrl(fileIdentifier, bucket, uri, expiration);
        }
    }
}
