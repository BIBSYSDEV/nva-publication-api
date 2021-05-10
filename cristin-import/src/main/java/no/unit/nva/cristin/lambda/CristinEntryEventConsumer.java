package no.unit.nva.cristin.lambda;

import static no.unit.nva.publication.s3imports.ApplicationConstants.MAX_SLEEP_TIME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.Random;
import no.unit.nva.cristin.lambda.dtos.CristinObjectEvent;
import no.unit.nva.cristin.lambda.dtos.ImportResult;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.s3imports.ApplicationConstants;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinEntryEventConsumer extends EventHandler<FileContentsEvent<CristinObject>, Publication> {

    public static final String WRONG_DETAIL_TYPE_ERROR_TEMPLATE =
        "Unexpected detail-type: %s. Expected detail-type is: %s.";
    public static final int MAX_EFFORTS = 10;
    public static final String ERROR_SAVING_CRISTIN_RESULT = "Could not save cristin result with ID: ";
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final String EVENT_DETAIL_TYPE = "import.cristin.entry-event";
    public static final String FILE_ENDING = ".json";
    public static final String EMPTY_FRAGMENT = null;
    private static final Logger logger = LoggerFactory.getLogger(CristinEntryEventConsumer.class);
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

    public static URI constructErrorFileUri(AwsEventBridgeEvent<FileContentsEvent<CristinObject>> event) {
        Path parentFolder = extractFolderPath(event.getDetail());
        String scheme = event.getDetail().getFileUri().getScheme();
        String bucketName = event.getDetail().getFileUri().getHost();
        String filename = event.getDetail().getContents().getId() + FILE_ENDING;
        Path filePath = Path.of(parentFolder.toString(), filename);
        return attempt(() -> new URI(scheme, bucketName, filePath.toString(), EMPTY_FRAGMENT)).orElseThrow();
    }

    @Override
    protected Publication processInput(FileContentsEvent<CristinObject> input,
                                       AwsEventBridgeEvent<FileContentsEvent<CristinObject>> event,
                                       Context context) {
        validateEvent(event);
        CristinObject cristinObject = extractCristinObject(input);
        Publication publication = cristinObject.toPublication();
        Try<Publication> attemptSave = persistInDatabase(publication);
        return attemptSave.orElseThrow(fail -> handleSavingError(fail, event));
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoDbClient() {
        return AmazonDynamoDBClientBuilder
                   .standard()
                   .withRegion(ApplicationConstants.AWS_REGION.id())
                   .build();
    }

    private static Path extractFolderPath(FileContentsEvent<CristinObject> event) {
        return Optional.of(event)
                   .map(FileContentsEvent::getFileUri)
                   .map(URI::getPath)
                   .map(Path::of)
                   .map(Path::getParent)
                   .orElseThrow();
    }

    private CristinObject extractCristinObject(FileContentsEvent<CristinObject> input) {
        CristinObject cristinObject = input.getContents().copy().build();
        cristinObject.setPublicationOwner(input.getPublicationsOwner());
        return cristinObject;
    }

    private void validateEvent(AwsEventBridgeEvent<FileContentsEvent<CristinObject>> event) {
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
                                               AwsEventBridgeEvent<FileContentsEvent<CristinObject>> event) {
        String errorMessage = ERROR_SAVING_CRISTIN_RESULT + event.getDetail().getContents().getId();
        logger.error(errorMessage, fail.getException());
        saveReportToS3(fail, event);
        return new RuntimeException(errorMessage, fail.getException());
    }

    private void saveReportToS3(Failure<Publication> fail,
                                AwsEventBridgeEvent<FileContentsEvent<CristinObject>> event) {
        URI errorFileUri = constructErrorFileUri(event);
        ImportResult<AwsEventBridgeEvent<FileContentsEvent<CristinObject>>> reportContent =
            ImportResult.reportFailure(event, fail.getException());
        S3Driver s3Driver = new S3Driver(s3Client, errorFileUri.getHost());
        s3Driver.insertFile(Path.of(errorFileUri.getPath()), reportContent.toJsonString());
    }
}
