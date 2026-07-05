package no.unit.nva.publication.file.text;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The payload placed on the SQS queue by the seeder or by any S3-event transformer. {@code
 * contentType} is resolved by the producer so the extraction handler does not need to issue a
 * HeadObject call per message.
 */
public record TextExtractionRequest(
    @JsonProperty("bucket") String bucket,
    @JsonProperty("key") String key,
    @JsonProperty("etag") String etag,
    @JsonProperty("contentType") String contentType) {}
