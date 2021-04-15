package no.unit.nva.publication.migration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class DataMigrationRequest implements JsonSerializable {

    public static final String PATH_DELIMITER = "/";

    @JsonProperty("s3Location")
    private URI s3Location;

    @JsonCreator
    public DataMigrationRequest(@JsonProperty("s3Location") String s3Location) {
        this.s3Location = Optional.ofNullable(s3Location).map(URI::create).orElse(null);
    }

    @JacocoGenerated
    public String getS3Location() {
        return Optional.ofNullable(s3Location).map(URI::toString).orElse(null);
    }

    @JacocoGenerated
    public void setS3Location(String s3Location) {
        this.s3Location = URI.create(s3Location);
    }

    public String extractBucketFromS3Location() {
        return s3Location.getHost();
    }

    public String extractPathFromS3Location() {
        return Optional.ofNullable(s3Location)
                   .map(URI::getPath)
                   .map(this::removeRoot)
                   .orElse(null);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getS3Location());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataMigrationRequest)) {
            return false;
        }
        DataMigrationRequest that = (DataMigrationRequest) o;
        return Objects.equals(getS3Location(), that.getS3Location());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }

    private String removeRoot(String path) {
        return path.startsWith(PATH_DELIMITER) ? path.substring(1) : path;
    }
}
