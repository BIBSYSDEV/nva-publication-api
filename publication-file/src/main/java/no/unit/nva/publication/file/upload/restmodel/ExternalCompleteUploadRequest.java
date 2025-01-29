package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.time.Instant;
import nva.commons.apigateway.exceptions.BadRequestException;

@JsonTypeName(ExternalCompleteUploadRequest.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record ExternalCompleteUploadRequest(String uploadId, String key, List<CompleteUploadPart> parts,
                                            String fileType, String name,
                                            String mimeType, Long size, URI license, PublisherVersion publisherVersion,
                                            Instant embargoDate, UploadDetails uploadDetails,
                                            RightsRetentionStrategy rightsRetentionStrategy)
    implements CompleteUploadRequest {

    public static final String TYPE = "ExternalCompleteUpload";

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

    public File toFile() {
        return null;
    }

    private List<PartETag> partETags() {
        return parts().stream().filter(CompleteUploadPart::hasValue).map(CompleteUploadPart::toPartETag).toList();
    }
}
