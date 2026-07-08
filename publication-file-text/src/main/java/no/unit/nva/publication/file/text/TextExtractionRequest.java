package no.unit.nva.publication.file.text;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;

/**
 * The payload placed on the text extraction SQS queue. The only current producer is {@link
 * SeedTextExtractionHandler}, which bulk-enqueues keys from an uploaded CSV; event-driven producers
 * reacting to uploads and deletions in the source bucket are planned but do not yet exist. The
 * consuming handler resolves content type and ETag via a HeadObject call, so producers only need to
 * supply the object coordinates.
 */
public record TextExtractionRequest(
    @JsonProperty("bucket") String bucket, @JsonProperty("key") String key)
    implements JsonSerializable {}
