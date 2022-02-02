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
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UnixPath;

/**
 * An {@link ImportRequest} contains 3 fields.
 *
 * <p>1. The {@link ImportRequest#s3Location} field is a URI to an S3 bucket of the form "s3://somebucket/some/path/".
 */
public class ImportRequest implements JsonSerializable {

    public static final String ILLEGAL_ARGUMENT_MESSAGE = "Illegal argument:";
    public static final String S3_LOCATION_FIELD = "s3Location";
    public static final String MISSING_S3_LOCATION_MESSAGE = S3_LOCATION_FIELD + " cannot be empty";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String SUBTOPIC = "subtopic";
    private static final String TOPIC = "topic";
    @JsonProperty(TOPIC)
    private final String topic;
    @JsonProperty(S3_LOCATION_FIELD)
    private final URI s3Location;
    @JsonProperty(TIMESTAMP_FIELD)
    private final Instant timestamp;
    @JsonProperty(SUBTOPIC)
    private final String subtopic;

    @JsonCreator
    public ImportRequest(
        @JsonProperty(TOPIC) String topic,
        @JsonProperty(SUBTOPIC) String subtopic,
        @JsonProperty(S3_LOCATION_FIELD) URI s3location,
        @JsonProperty(TIMESTAMP_FIELD) Instant timestamp) {
        this.s3Location = requireNonEmptyS3Location(s3location);
        this.timestamp = timestamp;
        this.topic = topic;
        this.subtopic = subtopic;
    }

    public static ImportRequest fromJson(String jsonString) {
        return attempt(() -> s3ImportsMapper.readValue(jsonString, ImportRequest.class))
            .orElseThrow(fail -> handleNotParsableInputError(fail, jsonString));
    }

    public String getSubtopic() {
        return subtopic;
    }

    public String getTopic() {
        return topic;
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

    protected ImportRequest withTopic(String topic) {
        return new ImportRequest(topic, subtopic, s3Location, timestamp);
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
        return Objects.hash(getTopic(), getS3Location(), getTimestamp());
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
        return Objects.equals(getTopic(), that.getTopic())
               && Objects.equals(getS3Location(), that.getS3Location())
               && Objects.equals(getTimestamp(), that.getTimestamp());
    }

    private static IllegalArgumentException handleNotParsableInputError(
        Failure<ImportRequest> fail, String inputString) {
        return new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE + inputString,fail.getException());
    }

    private URI requireNonEmptyS3Location(URI s3Location) {
        if (isNull(s3Location)) {
            throw new IllegalArgumentException(MISSING_S3_LOCATION_MESSAGE);
        }
        return s3Location;
    }
}
