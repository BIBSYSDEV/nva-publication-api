package no.unit.nva.publication.download;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.publication.download.exception.S3ServiceException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(PresignedUri.TYPE)
public record PresignedUri(UUID fileIdentifier, @JsonIgnore String bucket, Instant expires, String mime,
                           URI signedUri) {

    public static final String TYPE = "PresignedUrl";

    public static PresignedUri fromS3Key(UUID fileIdentifier) {
        return PresignedUri.builder().withFileIdentifier(fileIdentifier).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public PresignedUri bucket(String bucket) {
        return this.copy().withBucket(bucket).build();
    }

    public Builder copy() {
        return builder().withFileIdentifier(this.fileIdentifier)
                   .withBucket(this.bucket)
                   .withExpiration(this.expires);
    }

    public PresignedUri fromS3PresignerResponse(PresignedGetObjectRequest response) {
        return PresignedUri.builder()
                   .withFileIdentifier(fileIdentifier)
                   .withSignedUri(attempt(() -> response.url().toURI()).orElseThrow())
                   .withExpiration(response.expiration())
                   .build();
    }

    public PresignedUri create(S3Presigner s3Presigner) throws S3ServiceException {
        return attempt(() -> getS3ObjectRequest(fileIdentifier)).map(this::getPresignedUrlRequest)
                   .map(s3Presigner::presignGetObject)
                   .map(this::fromS3PresignerResponse)
                   .orElseThrow(e -> new S3ServiceException(e.toString(), e.getException()));
    }

    private GetObjectPresignRequest getPresignedUrlRequest(GetObjectRequest getObjectReqeust) {
        return GetObjectPresignRequest.builder()
                   .signatureDuration(Duration.ofMinutes(10))
                   .getObjectRequest(getObjectReqeust)
                   .build();
    }

    private GetObjectRequest getS3ObjectRequest(UUID fileIdentifier) {
        return GetObjectRequest.builder().bucket(bucket).key(fileIdentifier.toString()).responseContentType(mime).build();
    }

    public static final class Builder {

        private UUID fileIdentifier;
        private URI signedUri;
        private Instant expiration;
        private String bucket;
        private String mime;

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

        public Builder withMime(String mime) {
            this.mime = mime;
            return this;
        }

        public Builder withSignedUri(URI signedUri) {
            this.signedUri = signedUri;
            return this;
        }

        public Builder withExpiration(Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        public PresignedUri build() {
            return new PresignedUri(fileIdentifier, bucket, expiration, mime, signedUri);
        }
    }
}
