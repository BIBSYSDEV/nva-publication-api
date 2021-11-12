package no.unit.nva.publication.s3imports;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.paths.UnixPath;

/**
 * An {@link ImportRequest} contains 3 fields.
 *
 * <p>1. The {@link ImportRequest#s3Location} field is a URI to an S3 bucket of the form "s3://somebucket/some/path/".
 *
 * <p>2. The {@link ImportRequest#importEventType} field is a String denoting the event type that is going to be
 * created for each entry contained in the files of the folder. Pay attention that each file may contain multiple
 * entries.
 */
public class ImportRequest implements JsonSerializable {

    public static final String ILLEGAL_ARGUMENT_MESSAGE = "Illegal argument:";
    public static final String S3_LOCATION_FIELD = "s3Location";
    public static final String MISSING_S3_LOCATION_MESSAGE = S3_LOCATION_FIELD + " cannot be empty";
    public static final String IMPORT_EVENT_TYPE_FIELD = "importEventType";
    public static final String TIMESTAMP_FIELD = "timestamp";

    @JsonProperty(S3_LOCATION_FIELD)
    private final URI s3Location;
    // This field will be set as the event detail-type by the handler that emits one event per entry
    // and it will be expected by the specialized handler that will process the entry. E.g. CristinEntriesConsumer.
    @JsonProperty(IMPORT_EVENT_TYPE_FIELD)
    private final String importEventType;
    @JsonProperty(TIMESTAMP_FIELD)
    private final Instant timestamp;

    @JsonCreator
    public ImportRequest(@JsonProperty(S3_LOCATION_FIELD) URI s3location,
                         @JsonProperty(IMPORT_EVENT_TYPE_FIELD) String importEventType,
                         @JsonProperty(TIMESTAMP_FIELD) Instant timestamp) {
        this.s3Location = requireNonEmptyS3Location(s3location);
        this.importEventType = importEventType;
        this.timestamp = timestamp;
    }

    private URI requireNonEmptyS3Location(URI s3Location) {
        if (isNull(s3Location)) {
            throw new IllegalArgumentException(MISSING_S3_LOCATION_MESSAGE);
        }
        return s3Location;
    }

    public ImportRequest(URI s3location, Instant timestamp) {
        this(s3location, null, timestamp);
    }

    public static ImportRequest fromJson(String jsonString) {
        return attempt(() -> s3ImportsMapper.readValue(jsonString, ImportRequest.class))
            .orElseThrow(fail -> handleNotParsableInputError(jsonString));
    }

    public String getImportEventType() {
        return importEventType;
    }

    @JacocoGenerated
    public URI getS3Location() {
        return s3Location;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String extractBucketFromS3Location() {
        return s3Location.getHost();
    }

    public UnixPath extractPathFromS3Location() {
        return Optional.ofNullable(s3Location)
            .map(URI::getPath)
            .map(UnixPath::fromString)
            .map(UnixPath::removeRoot)
            .orElse(UnixPath.EMPTY_PATH);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getS3Location());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImportRequest)) {
            return false;
        }
        ImportRequest that = (ImportRequest) o;
        return Objects.equals(getS3Location(), that.getS3Location())
               && Objects.equals(getImportEventType(), that.getImportEventType());
    }

    private static IllegalArgumentException handleNotParsableInputError(String inputString) {
        return new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE + inputString);
    }
}
