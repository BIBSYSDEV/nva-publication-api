package no.sikt.nva.scopus;

import static java.util.Objects.nonNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class ScopusUnzipper {

    private static final Logger logger = LoggerFactory.getLogger(ScopusUnzipperHandler.class);
    private static final String SCOPUS_XML_BUCKET_NAME = new Environment().readEnv("SCOPUS_XML_BUCKET_NAME");
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
                var uploadedKey = extractAndUploadFile(entry, zipInputStream);
                sendMessage(uploadedKey);
            }
        } catch (IOException e) {
            logger.error("Could not unzip scopus file: {}", e.getMessage());
        }
        logger.info("Scopus file has been unzipped successfully");
    }

    private void sendMessage(String value) {
        var message = SendMessageRequest.builder()
                          .queueUrl(SCOPUS_IMPORT_QUEUE_URL)
                          .messageAttributes(Map.of("uri", MessageAttributeValue.builder().stringValue(value).build()))
                          .build();
        sqsClient.sendMessage(message);
    }


    private String extractAndUploadFile(ZipEntry entry, ZipInputStream zipInputStream) throws IOException {
        var buffer = new ByteArrayOutputStream();
        var tempBuffer = new byte[2048];
        int read;
        while ((read = zipInputStream.read(tempBuffer)) != -1) {
            buffer.write(tempBuffer, 0, read);
        }
        var targetKey = entry.getName();
        var request = PutObjectRequest.builder().bucket(SCOPUS_XML_BUCKET_NAME).key(targetKey).build();
        s3Client.putObject(request, RequestBody.fromBytes(buffer.toByteArray()));
        return targetKey;
    }
}
