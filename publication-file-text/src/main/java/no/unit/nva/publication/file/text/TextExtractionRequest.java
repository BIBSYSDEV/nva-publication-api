package no.unit.nva.publication.file.text;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The payload placed on the SQS queue by the seeder or by any S3-event transformer. The handler
 * resolves content type and ETag via a HeadObject call, so producers only need to supply the object
 * coordinates.
 */
public record TextExtractionRequest(
    @JsonProperty("bucket") String bucket, @JsonProperty("key") String key) {}
