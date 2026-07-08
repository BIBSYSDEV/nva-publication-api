package no.unit.nva.publication.file.text;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;

/**
 * A persisted marker recording that no text could be stored for a source object and why. Markers
 * are written to the text bucket under the {@code flags/} prefix so that affected files can be
 * enumerated, diagnosed, and redriven after a fix ships. {@code etag} identifies the source object
 * version the extraction attempt observed.
 */
public record ExtractionFlag(
    @JsonProperty("bucket") String bucket,
    @JsonProperty("key") String key,
    @JsonProperty("etag") String etag,
    @JsonProperty("reason") ExtractionFailureReason reason,
    @JsonProperty("detail") String detail)
    implements JsonSerializable {}
