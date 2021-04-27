package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;

public class ImportRequest implements JsonSerializable {

    public static final String PATH_DELIMITER = "/";
    public static final String S3_LOCATION_FIELD = "s3Location";
    private static final String EMPTY_STRING = "";

    @JsonProperty(S3_LOCATION_FIELD)
    private final URI s3Location;

    @JsonCreator
    public ImportRequest(@JsonProperty(S3_LOCATION_FIELD) String s3location) {
        this.s3Location = Optional.ofNullable(s3location).map(URI::create).orElseThrow();
    }

    public static ImportRequest fromJson(String jsonString) {
        return attempt(() -> JsonUtils.objectMapperWithEmpty.readValue(jsonString, ImportRequest.class))
                   .orElseThrow();
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
        ImportRequest request = (ImportRequest) o;
        return Objects.equals(getS3Location(), request.getS3Location());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }

    public InputStream toInputStream() {
        return IoUtils.stringToStream(toJsonString());
    }

    private String removeRoot(String path) {
        return path.startsWith(PATH_DELIMITER) ? path.substring(1) : path;
    }
}
