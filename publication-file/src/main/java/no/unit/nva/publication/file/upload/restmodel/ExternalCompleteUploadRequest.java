package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import nva.commons.apigateway.exceptions.BadRequestException;

@JsonTypeName(ExternalCompleteUploadRequest.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record ExternalCompleteUploadRequest(String uploadId, String key, List<CompleteUploadPart> parts,
                                            String fileType, URI license,
                                            PublisherVersion publisherVersion, Instant embargoDate)
    implements CompleteUploadRequest {

    public static final String TYPE = "ExternalCompleteUpload";

    @Override
    public CompleteMultipartUploadRequest toCompleteMultipartUploadRequest(String bucketName) {
        return new CompleteMultipartUploadRequest().withBucketName(bucketName)
                   .withKey(key())
                   .withUploadId(uploadId())
                   .withPartETags(partETags());
    }

    @Override
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
