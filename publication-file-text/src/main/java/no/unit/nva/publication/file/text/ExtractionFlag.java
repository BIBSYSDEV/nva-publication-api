package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.commons.json.JsonSerializable;

/**
 * A persisted marker recording that extraction did not complete cleanly for a source object and
 * why. Markers are written to the text bucket under the {@code flags/} prefix so that affected
 * files can be enumerated, diagnosed, and redriven. For {@link
 * ExtractionFailureReason#TRUNCATED_CONTENT} a partial {@code .txt} coexists with the marker by
 * design; every other reason implies no current text is stored. All fields are guaranteed
 * non-empty: construct via {@link #from}, which substitutes a named placeholder for blank detail.
 * {@code etag} identifies the source object version the extraction attempt read.
 */
public record ExtractionFlag(
    @JsonProperty("bucket") String bucket,
    @JsonProperty("key") String key,
    @JsonProperty("etag") String etag,
    @JsonProperty("reason") ExtractionFailureReason reason,
    @JsonProperty("detail") String detail)
    implements JsonSerializable {

  private static final String UNSPECIFIED_DETAIL = "UNSPECIFIED";

  public static ExtractionFlag from(
      ExtractionInput source, ExtractionFailureReason reason, String detail) {
    return new ExtractionFlag(
        source.sourceBucket(),
        source.sourceKey(),
        source.sourceEtag(),
        reason,
        nonBlankDetail(detail));
  }

  private static String nonBlankDetail(String detail) {
    return isNull(detail) || detail.isBlank() ? UNSPECIFIED_DETAIL : detail;
  }
}
