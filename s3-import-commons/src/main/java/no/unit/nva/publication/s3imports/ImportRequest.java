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

public class ImportRequest implements JsonSerializable {

    public static final String ILLEGAL_ARGUMENT_MESSAGE = "Illegal argument:";
    public static final String PATH_DELIMITER = "/";
    public static final String S3_LOCATION_FIELD = "s3Location";
    public static final String PUBLICATIONS_OWNER = "publicationsOwner";

    @JsonProperty(S3_LOCATION_FIELD)
    private final URI s3Location;
    @JsonProperty(PUBLICATIONS_OWNER)
    private final String publicationsOwner;

    @JsonCreator
    public ImportRequest(@JsonProperty(S3_LOCATION_FIELD) String s3location,
                         @JsonProperty(PUBLICATIONS_OWNER) String publicationsOwner) {
        this.s3Location = Optional.ofNullable(s3location).map(URI::create).orElseThrow();
        this.publicationsOwner = Optional.ofNullable(publicationsOwner).orElseThrow();
    }

    public ImportRequest(URI s3location, String owner) {
        this.s3Location = s3location;
        this.publicationsOwner = owner;
    }

    public static ImportRequest fromJson(String jsonString) {
        return attempt(() -> JsonUtils.objectMapperWithEmpty.readValue(jsonString, ImportRequest.class))
                   .orElseThrow(fail -> handleNotParsableInputError(jsonString));
    }

    public String getPublicationsOwner() {
        return publicationsOwner;
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

    private static IllegalArgumentException handleNotParsableInputError(String inputString) {
        return new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE + inputString);
    }

    private String removeRoot(String path) {
        return path.startsWith(PATH_DELIMITER) ? path.substring(1) : path;
    }
}
