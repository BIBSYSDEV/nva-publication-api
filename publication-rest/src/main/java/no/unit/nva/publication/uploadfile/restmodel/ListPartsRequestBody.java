package no.unit.nva.publication.uploadfile.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.ListPartsRequest;
import nva.commons.apigateway.exceptions.BadRequestException;

public record ListPartsRequestBody(String uploadId, String key) {

    public void validate() throws BadRequestException {
        try {
            requireNonNull(this.key());
            requireNonNull(this.uploadId());
        } catch (Exception e) {
            throw new BadRequestException("Invalid input");
        }
    }

    public ListPartsRequest toListPartsRequest(String bucketName) {
        return new ListPartsRequest(bucketName, key(), uploadId());
    }
}
