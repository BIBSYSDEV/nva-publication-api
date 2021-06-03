package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;

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
    public static final String PATH_DELIMITER = "/";
    public static final String S3_LOCATION_FIELD = "s3Location";
    public static final String IMPORT_EVENT_TYPE = "importEventType";

    @JsonProperty(S3_LOCATION_FIELD)
    private final URI s3Location;
    // This field will be set as the event detail-type by the handler that emits one event per entry
    // and it will be expected by the specialized handler that will process the entry. E.g. DataMigrationHandler.
    @JsonProperty(IMPORT_EVENT_TYPE)
    private final String importEventType;

    @JsonCreator
    public ImportRequest(@JsonProperty(S3_LOCATION_FIELD) String s3location,
                         @JsonProperty(IMPORT_EVENT_TYPE) String importEventType) {
        this.s3Location = Optional.ofNullable(s3location).map(URI::create).orElseThrow();
        this.importEventType = importEventType;
    }

    public ImportRequest(URI s3location, String importEventType) {
        this.s3Location = s3location;
        this.importEventType = importEventType;
    }

    public ImportRequest(URI s3Location) {
        this(s3Location, null);
    }

    public static ImportRequest fromJson(String jsonString) {
        return attempt(() -> JsonUtils.objectMapperWithEmpty.readValue(jsonString, ImportRequest.class))
                   .orElseThrow(fail -> handleNotParsableInputError(jsonString));
    }

    public String getImportEventType() {
        return importEventType;
    }

    @JacocoGenerated
    public String getS3Location() {
        return Optional.ofNullable(s3Location).map(URI::toString).orElse(null);
    }

    public String extractBucketFromS3Location() {
        return s3Location.getHost();
    }

    public String extractPathFromS3Location() {
        return Optional.ofNullable(s3Location)
                   .map(URI::getPath)
                   .map(this::removeRoot)
                   .orElse(EMPTY_STRING);
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

    private String removeRoot(String path) {
        return path.startsWith(PATH_DELIMITER) ? path.substring(1) : path;
    }
}
