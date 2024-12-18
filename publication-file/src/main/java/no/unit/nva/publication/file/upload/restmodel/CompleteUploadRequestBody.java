package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.PartETag;
import java.util.List;
import nva.commons.apigateway.exceptions.BadRequestException;

public record CompleteUploadRequestBody(String uploadId, String key, List<CompleteUploadPart> parts) {

    public List<PartETag> partETags() {
        return parts().stream().filter(CompleteUploadPart::hasValue).map(CompleteUploadPart::toPartETag).toList();
    }

    public CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(String bucketName) {
        return new CompleteMultipartUploadRequest()
                   .withBucketName(bucketName)
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
}
