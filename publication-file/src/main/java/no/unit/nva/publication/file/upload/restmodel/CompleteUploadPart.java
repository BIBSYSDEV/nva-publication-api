package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.PartETag;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public class CompleteUploadPart implements JsonSerializable {

    private Integer partNumber;
    private String etag;

    @JsonCreator
    public CompleteUploadPart(@JsonProperty("partNumber") @JsonAlias("PartNumber") Integer partNumber,
                              @JsonProperty("etag") @JsonAlias("ETag") String etag) {
        this.partNumber = partNumber;
        this.etag = etag;
    }

    public static PartETag toPartETag(CompleteUploadPart completeUploadPart) {
        return new PartETag(completeUploadPart.partNumber(), completeUploadPart.etag());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(partNumber, etag);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompleteUploadPart that)) {
            return false;
        }
        return Objects.equals(partNumber, that.partNumber) && Objects.equals(etag, that.etag);
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public boolean hasValue() {
        try {
            requireNonNull(etag);
            requireNonNull(partNumber);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String etag() {
        return etag;
    }

    private int partNumber() {
        return partNumber;
    }
}
