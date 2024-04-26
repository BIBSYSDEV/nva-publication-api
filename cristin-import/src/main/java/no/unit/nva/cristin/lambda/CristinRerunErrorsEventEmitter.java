package no.unit.nva.cristin.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.s3imports.PutSqsMessageResult;
import no.unit.nva.publication.s3imports.SqsBatchMessenger;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinRerunErrorsEventEmitter implements RequestStreamHandler {

    public static final String INPUT_FIELD = "input";
    public static final String FILE_URI_FIELD = "fileUri";
    public static final String DATA_IMPORT_TOPIC = "PublicationService.DataImport.DataEntry";
    public static final String CRISTIN_DATA_ENTRY_SUBTOPIC = "PublicationService.CristinData.DataEntry";
    public static final Environment ENVIRONMENT = new Environment();
    public static final String SQS_BATCH_RESULT_MESSAGE = "Sqs batch result: {}";
    public static final String REPORTS_DELETED_MESSAGE = "Old reports have been deleted!";
    private static final Logger logger = LoggerFactory.getLogger(CristinRerunErrorsEventEmitter.class);
    private static final String CRISTIN_IMPORT_BUCKET = ENVIRONMENT.readEnv("CRISTIN_IMPORT_BUCKET");
    private static final String CRISTIN_ENTRY_QUEUE = ENVIRONMENT.readEnv("CRISTIN_IMPORT_DATA_ENTRY_QUEUE_URL");
    private final S3Driver s3Driver;
    private final SqsBatchMessenger batchMessenger;

    @JacocoGenerated
    public CristinRerunErrorsEventEmitter() {
        this.s3Driver = new S3Driver(S3Driver.defaultS3Client().build(), CRISTIN_IMPORT_BUCKET);
        this.batchMessenger = new SqsBatchMessenger(defaultAmazonSQS(), CRISTIN_ENTRY_QUEUE);
    }

    public CristinRerunErrorsEventEmitter(S3Client s3Client, AmazonSQS sqsClient) {
        this.s3Driver = new S3Driver(s3Client, CRISTIN_IMPORT_BUCKET);
        this.batchMessenger = new SqsBatchMessenger(sqsClient, CRISTIN_ENTRY_QUEUE);
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        var event = getRerunFailedEntriesEvent(inputStream);
        var errorReports = s3Driver.listAllFiles(event.s3Path());
        var batchResult = putMessagesOnQueue(errorReports);

        logger.info(SQS_BATCH_RESULT_MESSAGE, batchResult.toJsonString());

        errorReports.forEach(s3Driver::deleteFile);

        logger.info(REPORTS_DELETED_MESSAGE);
    }

    @JacocoGenerated
    private static AmazonSQS defaultAmazonSQS() {
        return AmazonSQSClientBuilder.defaultClient();
    }

    private static RerunFailedEntriesEvent getRerunFailedEntriesEvent(InputStream inputStream) {
        return attempt(() -> IoUtils.streamToString(inputStream)).map(
            CristinRerunErrorsEventEmitter::toRerunFailedEntriesEvent).orElseThrow();
    }

    private static JsonNode getInput(JsonNode jsonNode) {
        return jsonNode.get(INPUT_FIELD);
    }

    private static JsonNode getFileUri(JsonNode jsonNode) {
        return jsonNode.get(FILE_URI_FIELD);
    }

    private static EventReference toEventReference(URI uri) {
        return new EventReference(DATA_IMPORT_TOPIC, CRISTIN_DATA_ENTRY_SUBTOPIC, uri);
    }

    private static RerunFailedEntriesEvent toRerunFailedEntriesEvent(String value) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(value, RerunFailedEntriesEvent.class);
    }

    private PutSqsMessageResult putMessagesOnQueue(List<UnixPath> reportLocationList) {
        return reportLocationList.stream()
                   .map(s3Driver::getFile)
                   .map(this::getFileLocation)
                   .map(CristinRerunErrorsEventEmitter::toEventReference)
                   .collect(Collectors.collectingAndThen(Collectors.toList(),
                                                         list -> list.isEmpty() ? new PutSqsMessageResult()
                                                                      : batchMessenger.sendMessages(list)));
    }

    private URI getFileLocation(String content) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(content)).map(CristinRerunErrorsEventEmitter::getInput)
                   .map(CristinRerunErrorsEventEmitter::getFileUri)
                   .map(JsonNode::asText)
                   .map(URI::new)
                   .orElseThrow();
    }
}
