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
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.s3imports.SqsBatchMessenger;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinRerunErrorsEventEmitter implements RequestStreamHandler {

    public static final String INPUT_FIELD = "input";
    public static final String FILE_URI_FIELD = "fileUri";
    public static final String DATA_IMPORT_TOPIC = "PublicationService.DataImport.DataEntry";
    public static final String CRISTIN_DATA_ENTRY_SUBTOPIC = "PublicationService.CristinData.DataEntry";
    public static final Environment ENVIRONMENT = new Environment();
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
        s3Driver.getFiles(event.s3Path()).stream()
            .map(this::getFileLocation)
            .map(CristinRerunErrorsEventEmitter::toEventReference)
            .collect(Collectors.collectingAndThen(Collectors.toList(),
                                                  list -> (list.isEmpty() ? null : batchMessenger.sendMessages(list))));
    }

    @JacocoGenerated
    private static AmazonSQS defaultAmazonSQS() {
        return AmazonSQSClientBuilder.defaultClient();
    }

    private static RerunFailedEntriesEvent getRerunFailedEntriesEvent(InputStream inputStream) {
        return attempt(() -> IoUtils.streamToString(inputStream))
                   .map(CristinRerunErrorsEventEmitter::toRerunFailedEntriesEvent)
                   .orElseThrow();
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

    private URI getFileLocation(String content) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(content))
                   .map(CristinRerunErrorsEventEmitter::getInput)
                   .map(CristinRerunErrorsEventEmitter::getFileUri)
                   .map(JsonNode::asText)
                   .map(URI::new)
                   .orElseThrow();
    }
}
