package no.unit.nva.dataimport;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class ImportRequest implements JsonSerializable {

    public static final String PATH_DELIMITER = "/";
    @JsonProperty("s3location")
    private URI s3Location;
    @JsonProperty("table")
    private String table;

    //Default serializer necessary for AWS's serializer.
    @JacocoGenerated
    public ImportRequest() {

    }

    public ImportRequest(String s3location,
                         String table) {

        this.table = table;
        this.s3Location = URI.create(s3location);
    }

    @JacocoGenerated
    public String getS3Location() {
        return s3Location.toString();
    }

    @JacocoGenerated
    public void setS3Location(String s3Location) {
        this.s3Location = URI.create(s3Location);
    }

    @JacocoGenerated
    public String getTable() {
        return table;
    }

    @JacocoGenerated
    public void setTable(String table) {
        this.table = table;
    }

    public String extractBucketFromS3Location() {
        return s3Location.getHost();
    }

    public String extractPathFromS3Location() {
        String path = s3Location.getPath();
        return removeRoot(path);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getS3Location(), getTable());
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
        return Objects.equals(getS3Location(), request.getS3Location()) && Objects.equals(getTable(),
                                                                                          request.getTable());
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
