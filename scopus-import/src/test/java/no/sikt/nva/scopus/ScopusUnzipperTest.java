package no.sikt.nva.scopus;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class ScopusUnzipperTest {

    public static final String CONTENT_OF_PERSISTED_FILE = "content of persisted file";
    private S3Client s3Client;
    private SqsClient sqsClient;
    private ScopusUnzipper scopusUnzipper;

    @BeforeEach
    public void setup() {
        this.s3Client = new FakeS3Client();
        this.sqsClient = mock(SqsClient.class);
        this.scopusUnzipper = new ScopusUnzipper(s3Client, sqsClient);
    }

    @Test
    void shouldUnzipS3ObjectAndPlaceFilesOnQueue() throws IOException {
        var zipFileName = randomString() + ".zip";
        var bucketName = randomString();
        var filenamesInZip = List.of(randomString(), randomString(), randomString());
        var zipBytes = createSampleZipFile(filenamesInZip);

        new S3Driver(s3Client, bucketName).insertFile(UnixPath.of(zipFileName), new ByteArrayInputStream(zipBytes));

        scopusUnzipper.unzipAndEnqueue(bucketName, zipFileName);

        verify(sqsClient, times(3)).sendMessage(any(SendMessageRequest.class));

        filenamesInZip.stream()
            .map(this::getPersistedFileFromXmlBucket)
            .forEach(value -> assertEquals(CONTENT_OF_PERSISTED_FILE, value));
    }

    private String getPersistedFileFromXmlBucket(String name) {
        return new S3Driver(s3Client, new Environment().readEnv("SCOPUS_XML_BUCKET_NAME")).getFile(UnixPath.of(name));
    }

    private byte[] createSampleZipFile(List<String> filenames) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        try (var zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (String filename : filenames) {
                var entry = new ZipEntry(filename);
                zipOutputStream.putNextEntry(entry);
                zipOutputStream.write(CONTENT_OF_PERSISTED_FILE.getBytes());
                zipOutputStream.closeEntry();
            }
        } return byteArrayOutputStream.toByteArray();
    }
}