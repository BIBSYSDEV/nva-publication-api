package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import nva.commons.apigateway.exceptions.BadRequestException;

@JsonTypeName(InternalCompleteUploadRequest.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record InternalCompleteUploadRequest(String uploadId, String key, List<CompleteUploadPart> parts)
    implements CompleteUploadRequest {

    public static final String TYPE = "InternalCompleteUpload";

    public CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(String bucketName) {
        return new CompleteMultipartUploadRequest().withBucketName(bucketName)
                   .withKey(key())
                   .withUploadId(uploadId())
                   .withPartETags(partETags());
    }

    public void validate() throws BadRequestException {
        try {
            requireNonNull(this.uploadId());
            requireNonNull(this.key());
            requireNonNull(this.parts());
        } catch (Exception e) {
            throw new BadRequestException("Invalid input");
        }
    }

    private List<PartETag> partETags() {
        return parts().stream().filter(CompleteUploadPart::hasValue).map(CompleteUploadPart::toPartETag).toList();
    }
}
