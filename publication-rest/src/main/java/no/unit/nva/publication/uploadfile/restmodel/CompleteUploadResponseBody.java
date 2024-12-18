package no.unit.nva.publication.uploadfile.restmodel;

import com.amazonaws.services.s3.model.ObjectMetadata;
import java.util.regex.Pattern;

public record CompleteUploadResponseBody(String location, String identifier, String fileName, String mimeType,
                                         long size) {

    public static final String FILE_NAME_REGEX = "filename=\"(.*)\"";

    public static CompleteUploadResponseBody create(ObjectMetadata objectMetadata, String key) {
        return new CompleteUploadResponseBody(key,
                                              key,
                                              toFileName(objectMetadata.getContentDisposition()),
                                              objectMetadata.getContentType(),
                                              objectMetadata.getContentLength());
    }

    public static String toFileName(String contentDisposition) {
        var pattern = Pattern.compile(FILE_NAME_REGEX);
        var matcher = pattern.matcher(contentDisposition);
        return matcher.matches() ? matcher.group(1) : contentDisposition;
    }
}
