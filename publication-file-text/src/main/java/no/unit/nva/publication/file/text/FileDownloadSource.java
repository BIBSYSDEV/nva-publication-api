package no.unit.nva.publication.file.text;

import java.io.IOException;

/**
 * Port for downloading a stored object to a local temp file. Returns the file together with the
 * ETag of the object version that was read. Throws {@link
 * software.amazon.awssdk.services.s3.model.NoSuchKeyException} when the object does not exist,
 * {@link FileTooLargeException} — before any bytes are transferred — when the object exceeds the
 * implementation's size limit, and {@link IOException} on transfer failure; implementations must
 * not leave a temp file behind on failure.
 */
@FunctionalInterface
public interface FileDownloadSource {

  DownloadedObject downloadToFile(String bucket, String key) throws IOException;
}
