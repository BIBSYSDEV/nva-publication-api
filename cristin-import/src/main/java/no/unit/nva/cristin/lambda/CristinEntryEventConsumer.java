package no.unit.nva.cristin.lambda;

import static no.unit.nva.publication.s3imports.ApplicationConstants.MAX_SLEEP_TIME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import no.unit.nva.cristin.lambda.dtos.CristinObjectEvent;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.s3imports.ApplicationConstants;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinEntryEventConsumer extends EventHandler<FileContentsEvent<JsonNode>, Publication> {

    public static final String WRONG_DETAIL_TYPE_ERROR_TEMPLATE =
        "Unexpected detail-type: %s. Expected detail-type is: %s.";
    public static final int MAX_EFFORTS = 10;
    public static final String ERROR_SAVING_CRISTIN_RESULT = "Could not save cristin result with ID: ";
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final String EVENT_DETAIL_TYPE = "import.cristin.entry-event";
    public static final String FILE_ENDING = ".json";
    public static final String EMPTY_FRAGMENT = null;
    public static final String UNKNOWN_CRISTIN_ID = "unknownCristinId_";
    public static final String DO_NOT_WRITE_ID_IN_EXPCETION_MESSAGE = null;
    private static final Logger logger = LoggerFactory.getLogger(CristinEntryEventConsumer.class);
    public static final String ERRORS_FOLDER = "errors";
    private final ResourceService resourceService;
    private final S3Client s3Client;

    @JacocoGenerated
    public CristinEntryEventConsumer() {
        this(defaultDynamoDbClient(), S3Client.builder().build());
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

    private static URI constructErrorFileUri(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        Path filePath = constructErrorFilePathInsideTheBucket(event);
        String uriScheme = event.getDetail().getFileUri().getScheme();
        String bucketAsUriHost = event.getDetail().getFileUri().getHost();
        return attempt(() -> new URI(uriScheme, bucketAsUriHost, filePath.toString(), EMPTY_FRAGMENT)).orElseThrow();
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

    private static CristinObject parseCristinObject(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        CristinObject cristinObject = jsonNodeToCristinObject(event);
        cristinObject.hardcodePublicationOwner(event.getDetail().getPublicationsOwner());
        return cristinObject;
    }

    private static CristinObject jsonNodeToCristinObject(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return attempt(() -> event.getDetail().getContents())
                   .map(jsonNode -> JsonUtils.objectMapperNoEmpty.convertValue(jsonNode, CristinObject.class))
                   .orElseThrow();
    }

    private static Path constructErrorFilePathInsideTheBucket(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        Path parentFolder = extractFolderPath(event.getDetail());
        String filename = createErrorReportFilename(event) + FILE_ENDING;
        return Path.of(parentFolder.toString(), ERRORS_FOLDER, filename);
    }

    private static String createErrorReportFilename(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return extractCristinObjectId(event).orElse(unknownCristinIdReportFilename());
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoDbClient() {
        return AmazonDynamoDBClientBuilder
                   .standard()
                   .withRegion(ApplicationConstants.AWS_REGION.id())
                   .build();
    }

    private static Path extractFolderPath(FileContentsEvent<JsonNode> event) {
        return Optional.of(event)
                   .map(FileContentsEvent::getFileUri)
                   .map(URI::getPath)
                   .map(Path::of)
                   .map(Path::getParent)
                   .orElseThrow();
    }

    private static Optional<String> extractCristinObjectId(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        return attempt(() -> parseCristinObject(event))
                   .map(CristinObject::getId)
                   .map(Objects::toString)
                   .toOptional();
    }

    private static String unknownCristinIdReportFilename() {
        return UNKNOWN_CRISTIN_ID + UUID.randomUUID();
    }

    private void validateEvent(AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        if (!EVENT_DETAIL_TYPE.equals(event.getDetailType())) {
            String errorMessage = String.format(WRONG_DETAIL_TYPE_ERROR_TEMPLATE,
                                                event.getDetailType(),
                                                EVENT_DETAIL_TYPE);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private Try<Publication> persistInDatabase(Publication publication) {
        Try<Publication> attemptSave = tryPersistingInDatabase(publication);

        for (int efforts = 0; shouldTryAgain(attemptSave, efforts); efforts++) {
            attemptSave = tryPersistingInDatabase(publication);
            sleep(RANDOM.nextInt(MAX_SLEEP_TIME));
        }
        return attemptSave;
    }

    private boolean shouldTryAgain(Try<Publication> attemptSave, int efforts) {
        return attemptSave.isFailure() && efforts < MAX_EFFORTS;
    }

    private Try<Publication> tryPersistingInDatabase(Publication publication) {
        return attempt(() -> resourceService.createPublicationWithPredefinedCreationDate(publication));
    }

    private void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private RuntimeException handleSavingError(Failure<Publication> fail,
                                               AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        String cristinObjectId = extractCristinObjectId(event).orElse(DO_NOT_WRITE_ID_IN_EXPCETION_MESSAGE);
        String errorMessage = ERROR_SAVING_CRISTIN_RESULT + cristinObjectId;
        logger.error(errorMessage, fail.getException());
        saveReportToS3(fail, event);
        return new RuntimeException(errorMessage, fail.getException());
    }

    private void saveReportToS3(Failure<Publication> fail,
                                AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event) {
        URI errorFileUri = constructErrorFileUri(event);
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getHost());
        ImportResult<AwsEventBridgeEvent<FileContentsEvent<JsonNode>>> reportContent =
            ImportResult.reportFailure(event, fail.getException());
        s3Driver.insertFile(Path.of(errorFileUri.getPath()), reportContent.toJsonString());
    }
}
