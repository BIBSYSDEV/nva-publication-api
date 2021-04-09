package no.unit.nva.dataimport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImportRequest {

    @JsonProperty("bucketName")
    private final String bucketName;
    @JsonProperty("folderPath")
    private final String folderPath;
    @JsonProperty("table")
    private final String table;

    @JsonCreator
    public ImportRequest(@JsonProperty("bucketName") String bucketName,
                         @JsonProperty("folderPath") String folderPath,
                         @JsonProperty("table") String table) {
        this.bucketName = bucketName;
        this.folderPath = folderPath;
        this.table = table;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getTable() {
        return table;
    }
}
