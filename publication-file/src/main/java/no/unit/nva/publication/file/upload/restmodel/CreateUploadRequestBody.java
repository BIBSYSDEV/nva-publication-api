package no.unit.nva.publication.file.upload.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.net.MediaType;
import java.util.UUID;
import no.unit.nva.publication.file.upload.Filename;
import nva.commons.apigateway.exceptions.BadRequestException;

public record CreateUploadRequestBody(String filename, String size, String mimetype) {

    public InitiateMultipartUploadRequest toInitiateMultipartUploadRequest(String bucketName) {
        var key = UUID.randomUUID().toString();
        return new InitiateMultipartUploadRequest(bucketName, key, constructObjectMetadata());
    }

    public void validate() throws BadRequestException {
        try {
            requireNonNull(this.filename());
            requireNonNull(this.size());
            MediaType.parse(this.mimetype());
        } catch (Exception e) {
            throw new BadRequestException("Invalid input");
        }
    }

    private ObjectMetadata constructObjectMetadata() {
        var objectMetadata = new ObjectMetadata();
        objectMetadata.setContentMD5(null);
        objectMetadata.setContentDisposition(Filename.toContentDispositionValue(filename()));
        objectMetadata.setContentType(mimetype());
        return objectMetadata;
    }
}
