package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.PartETag;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CompleteUploadPart(@JsonProperty("PartNumber") Integer partNumber, @JsonProperty("ETag") String etag) {

    public static PartETag toPartETag(CompleteUploadPart completeUploadPart) {
        return new PartETag(completeUploadPart.partNumber(), completeUploadPart.etag());
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
}
