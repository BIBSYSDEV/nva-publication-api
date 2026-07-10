package no.unit.nva.publication.file.text;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

/**
 * The payload placed on the text extraction SQS queue. The only current producer is {@link
 * SeedTextExtractionHandler}, which bulk-enqueues keys from an uploaded CSV; event-driven producers
 * reacting to uploads and deletions in the source bucket are planned but do not yet exist. The
 * consuming handler downloads the object and detects its content type from the bytes, so producers
 * only need to supply the object coordinates.
 */
public record TextExtractionRequest(
    @JsonProperty("bucket") String bucket, @JsonProperty("key") String key)
    implements JsonSerializable {

  private static final String UNPARSEABLE_MESSAGE_BODY = "Unparseable SQS message body: ";
  private static final String PARSE_LOCATION_TEMPLATE = " at line %d, column %d";
  private static final String EMPTY_STRING = "";

  /**
   * Parses a request from its JSON representation. Throws {@link IllegalArgumentException} when
   * {@code body} is not valid JSON for this type; the exception message carries a sanitized parser
   * diagnostic and parse location, never the raw body.
   */
  public static TextExtractionRequest fromJson(String body) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(body, TextExtractionRequest.class))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    UNPARSEABLE_MESSAGE_BODY + describeParseFailure(failure.getException())));
  }

  private static String describeParseFailure(Exception exception) {
    if (exception instanceof JsonProcessingException jsonException) {
      return LogSanitizer.sanitize(jsonException.getOriginalMessage())
          + parseLocationOf(jsonException);
    }
    return LogSanitizer.sanitize(exception.getMessage());
  }

  private static String parseLocationOf(JsonProcessingException jsonException) {
    var location = jsonException.getLocation();
    if (isNull(location) || JsonLocation.NA.equals(location)) {
      return EMPTY_STRING;
    }
    return PARSE_LOCATION_TEMPLATE.formatted(location.getLineNr(), location.getColumnNr());
  }
}
