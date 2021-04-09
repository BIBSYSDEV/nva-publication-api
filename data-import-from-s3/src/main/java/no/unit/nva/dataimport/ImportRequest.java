package no.unit.nva.dataimport;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

public class ImportRequest implements JsonSerializable {

    @JsonProperty("bucket")
    private String bucket;
    @JsonProperty("folderPath")
    private String folderPath;
    @JsonProperty("table")
    private String table;

    //Default serializer necessary for AWS's serializer.
    @JacocoGenerated
    public ImportRequest() {

    }

    public ImportRequest(String bucket,
                         String folderPath,
                         String table) {
        this.bucket = bucket;
        this.folderPath = folderPath;
        this.table = table;
    }

    public String getBucket() {
        return bucket;
    }

    @JacocoGenerated
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    @JacocoGenerated
    public String getFolderPath() {
        return folderPath;
    }

    @JacocoGenerated
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    @JacocoGenerated
    public String getTable() {
        return table;
    }

    @JacocoGenerated
    public void setTable(String table) {
        this.table = table;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}
