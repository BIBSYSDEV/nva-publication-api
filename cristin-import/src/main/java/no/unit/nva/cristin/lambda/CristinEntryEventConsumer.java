package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.CristinImportConfig.objectMapper;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_PUBLICATIONS_OWNER;
import static no.unit.nva.publication.s3imports.ApplicationConstants.MAX_SLEEP_TIME;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import no.unit.nva.cristin.lambda.dtos.CristinObjectEvent;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.Identifiable;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.s3imports.ApplicationConstants;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.s3imports.UriWrapper;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinEntryEventConsumer extends EventHandler<FileContentsEvent<JsonNode>, Publication> {

    public static final String WRONG_DETAIL_TYPE_ERROR_TEMPLATE =
        "Unexpected detail-type: %s. Expected detail-type is: %s.";
    public static final int MAX_EFFORTS = 10;
    public static final String ERROR_SAVING_CRISTIN_RESULT = "Could not save cristin result with ID: ";
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final String EVENT_DETAIL_TYPE = "import.cristin.entry-event";
    public static final String JSON = ".json";
    public static final String UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX = "unknownCristinId_";
    public static final String DO_NOT_WRITE_ID_IN_EXCEPTION_MESSAGE = null;
    public static final String ERRORS_FOLDER = "errors";

    private static final Logger logger = LoggerFactory.getLogger(CristinEntryEventConsumer.class);
    private final ResourceService resourceService;
    private final S3Client s3Client;

    @JacocoGenerated
    public CristinEntryEventConsumer() {
        this(defaultDynamoDbClient(), defaultS3Client());
    }

    @JacocoGenerated
    protected CristinEntryEventConsumer(AmazonDynamoDB dynamoDbClient, S3Client s3Client) {
        this(new ResourceService(dynamoDbClient, Clock.systemDefaultZone()), s3Client);
    }

    protected CristinEntryEventConsumer(ResourceService resourceService, S3Client s3Client) {
        super(CristinObjectEvent.class);
        this.resourceService = resourceService;
        this.s3Client = s3Client;
    }

    @Override
    protected Publication processInput(FileContentsEvent<JsonNode> input,
                                       AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event,
                                       Context context) {
        validateEvent(event);
        return attempt(() -> parseCristinObject(event))
                   .map(CristinObject::toPublication)
                   .flatMap(this::persistInDatabase)
                   .orElseThrow(fail -> handleSavingError(fail, event));
    }

    @JacocoGenerated
    private static S3Client defaultS3Client() {
        return S3Client.builder()
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoDbClient() {
        return AmazonDynamoDBClientBuilder
                   .standard()
                   .withRegion(ApplicationConstants.AWS_REGION.id())
                   .build();
    }

    private CristinObject parseCristinObject(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        CristinObject cristinObject = jsonNodeToCristinObject(event);
        cristinObject.hardcodePublicationOwner(HARDCODED_PUBLICATIONS_OWNER);
        return cristinObject;
    }

    private Identifiable parseIdentifiableObject(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {

        return attempt(() -> event.getDetail().getContents())
                   .map(jsonNode ->
                            objectMapper.convertValue(jsonNode, Identifiable.class))
                   .orElseThrow();
    }

    private CristinObject jsonNodeToCristinObject(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return attempt(() -> event.getDetail().getContents())
                   .map(CristinObject::fromJson)
                   .orElseThrow();
    }

    private void validateEvent(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        if (!EVENT_DETAIL_TYPE.equals(event.getDetailType())) {
            String errorMessage = messageIndicatingTheCorrectEventType(event);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String messageIndicatingTheCorrectEventType(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return String.format(WRONG_DETAIL_TYPE_ERROR_TEMPLATE, event.getDetailType(), EVENT_DETAIL_TYPE);
    }

    private Try<Publication> persistInDatabase(Publication publication) {
        Try<Publication> attemptSave = tryPersistingInDatabase(publication);

        for (int efforts = 0; shouldTryAgain(attemptSave, efforts); efforts++) {
            attemptSave = tryPersistingInDatabase(publication);

            avoidCongestionInDatabase();
        }
        return attemptSave;
    }

    private boolean shouldTryAgain(Try<Publication> attemptSave, int efforts) {
        return attemptSave.isFailure() && efforts < MAX_EFFORTS;
    }

    private Try<Publication> tryPersistingInDatabase(Publication publication) {
        return attempt(() -> createPublication(publication));
    }

    private Publication createPublication(Publication publication)
        throws TransactionFailedException {
        return resourceService.createPublicationWhilePersistingEntryFromLegacySystems(publication);
    }

    private void avoidCongestionInDatabase() {
        int sleepTime = spreadWriteRequests();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Randomize waiting time to avoiding creating parallel executions that perform retries in sync.
    private int spreadWriteRequests() {
        return RANDOM.nextInt(MAX_SLEEP_TIME);
    }

    private RuntimeException handleSavingError(Failure<Publication> fail,
                                               AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        String cristinObjectId = extractCristinObjectId(event).orElse(DO_NOT_WRITE_ID_IN_EXCEPTION_MESSAGE);
        String errorMessage = ERROR_SAVING_CRISTIN_RESULT + cristinObjectId;
        logger.error(errorMessage, fail.getException());

        saveReportToS3(fail, event);

        return new RuntimeException(errorMessage, fail.getException());
    }

    private void saveReportToS3(Failure<Publication> fail,
                                AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        UriWrapper errorFileUri = constructErrorFileUri(event, fail.getException());
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getUri().getHost());
        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> reportContent =
            ImportResult.reportFailure(event, fail.getException());
        s3Driver.insertFile(errorFileUri.toS3bucketPath(), reportContent.toJsonString());
    }

    private UriWrapper constructErrorFileUri(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event,
                                             Exception exception) {
        UriWrapper fileUri = new UriWrapper(event.getDetail().getFileUri());
        Instant timestamp = event.getDetail().getTimestamp();
        UriWrapper bucket = fileUri.getHost();
        return bucket
                   .addChild(ERRORS_FOLDER)
                   .addChild(timestampToString(timestamp))
                   .addChild(exception.getClass().getSimpleName())
                   .addChild(fileUri.getPath())
                   .addChild(createErrorReportFilename(event));
    }

    private String createErrorReportFilename(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return extractCristinObjectId(event)
                   .map(idString -> idString + JSON)
                   .orElseGet(this::unknownCristinIdReportFilename);
    }

    private Optional<String> extractCristinObjectId(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return attempt(() -> parseIdentifiableObject(event))
                   .map(Identifiable::getId)
                   .toOptional()
                   .map(Objects::toString);
    }

    private String unknownCristinIdReportFilename() {
        return UNKNOWN_CRISTIN_ID_ERROR_REPORT_PREFIX + UUID.randomUUID() + JSON;
    }
}
