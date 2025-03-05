package no.sikt.nva.scopus;

import static java.util.Objects.nonNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class ScopusUnzipper {

    private static final Logger logger = LoggerFactory.getLogger(ScopusUnzipperHandler.class);
    private static final String SCOPUS_XML_BUCKET_NAME = new Environment().readEnv("SCOPUS_XML_BUCKET");
    private static final String SCOPUS_IMPORT_QUEUE_URL = new Environment().readEnv("SCOPUS_IMPORT_QUEUE");
    private final S3Client s3Client;
    private final SqsClient sqsClient;

    public ScopusUnzipper(S3Client s3Client, SqsClient sqsClient) {
        this.s3Client = s3Client;
        this.sqsClient = sqsClient;
    }

    @JacocoGenerated
    public static ScopusUnzipper create() {
        return new ScopusUnzipper(S3Driver.defaultS3Client().build(), SqsClient.create());
    }

    public void unzipAndEnqueue(String bucket, String key) {
        var getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();
        var s3ClientObject = s3Client.getObject(getObjectRequest);

        try (var zipInputStream = new ZipInputStream(s3ClientObject)) {
            for (var entry = zipInputStream.getNextEntry(); nonNull(entry); entry = zipInputStream.getNextEntry()) {
                var location = extractAndUploadFile(entry, zipInputStream);
                sendMessage(location);
            }
        } catch (IOException e) {
            logger.error("Could not unzip scopus file: {}", e.getMessage());
        }
        logger.info("Scopus file has been unzipped successfully");
    }

    private void sendMessage(URI uri) {
        var message = SendMessageRequest.builder()
                          .queueUrl(SCOPUS_IMPORT_QUEUE_URL)
                          .messageBody(uri.toString())
                          .messageAttributes(
                              Map.of("uri", MessageAttributeValue.builder().stringValue(uri.toString()).build()))
                          .build();
        sqsClient.sendMessage(message);
    }

    private URI extractAndUploadFile(ZipEntry entry, ZipInputStream zipInputStream) throws IOException {
        var buffer = new ByteArrayOutputStream();
        var tempBuffer = new byte[2048];
        int read;
        while ((read = zipInputStream.read(tempBuffer)) != -1) {
            buffer.write(tempBuffer, 0, read);
        }
        return insertToS3(entry.getName(), new ByteArrayInputStream(buffer.toByteArray()));
    }

    private URI insertToS3(String key, InputStream inputStream) throws IOException {
        var s3Driver = new S3Driver(s3Client, SCOPUS_XML_BUCKET_NAME);
        return s3Driver.insertFile(UnixPath.of(key), inputStream);
    }
}
