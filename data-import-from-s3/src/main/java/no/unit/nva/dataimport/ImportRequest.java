package no.unit.nva.dataimport;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

public class ImportRequest {

    @JsonProperty("bucket")
    private String bucket;
    @JsonProperty("folderPath")
    private String folderPath;
    @JsonProperty("table")
    private String table;

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

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }
}
