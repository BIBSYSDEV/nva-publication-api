package no.unit.nva.publication.uploadfile.restmodel;

import static java.util.Objects.requireNonNull;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.net.MediaType;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.exceptions.BadRequestException;
import org.apache.commons.text.translate.UnicodeEscaper;

public record CreateUploadRequestBody(String filename, String size, String mimetype) {

    public static final String CONTENT_DISPOSITION_TEMPLATE = "filename=\"%s\"";
    public static final int LAST_ASCII_CODEPOINT = 127;

    public InitiateMultipartUploadRequest toInitiateMultipartUploadRequest(String bucketName) {
        var key = SortableIdentifier.next().toString();
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

    private String escapeFilename() {
        var unicodeEscaper = UnicodeEscaper.above(LAST_ASCII_CODEPOINT);
        return unicodeEscaper.translate(filename());
    }

    private ObjectMetadata constructObjectMetadata() {
        var objectMetadata = new ObjectMetadata();
        objectMetadata.setContentMD5(null);
        objectMetadata.setContentDisposition(extractFormattedContentDispositionForFilename());
        objectMetadata.setContentType(mimetype());
        return objectMetadata;
    }

    private String extractFormattedContentDispositionForFilename() {
        return String.format(CONTENT_DISPOSITION_TEMPLATE, escapeFilename());
    }
}
