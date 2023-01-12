package no.sikt.nva.brage.migration.lambda;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Iterables;
import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.AssociatedArtifactMover;
import no.sikt.nva.brage.migration.mapper.BrageNvaMapper;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class BrageEntryEventConsumer implements RequestHandler<S3Event, Publication> {

    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final int MAX_EFFORTS = 10;
    public static final String SOURCE_CRISTIN = "Cristin";
    public static final String BRAGE_MIGRATION_ERROR_BUCKET_NAME = "BRAGE_MIGRATION_ERROR_BUCKET_NAME";
    public static final String YYYY_MM_DD_HH_FORMAT = "yyyy-MM-dd:HH";
    public static final String ERROR_BUCKET_PATH = "ERROR";
    public static final String HANDLE_REPORTS_PATH = "HANDLE_REPORTS";
    public static final String PATH_SEPERATOR = "/";
    private static final int MAX_SLEEP_TIME = 100;
    private static final int SINGLE_EXPECTED_RECORD = 0;
    private static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final String ERROR_SAVING_BRAGE_IMPORT = "Error saving brage import for record with object key: ";
    private static final Logger logger = LoggerFactory.getLogger(BrageEntryEventConsumer.class);
    private final S3Client s3Client;
    private final ResourceService resourceService;
    private String brageRecordFile;
    private List<Publication> publicationsByCristinIdentifier;

    public BrageEntryEventConsumer(S3Client s3Client, ResourceService resourceService) {
        this.s3Client = s3Client;
        this.resourceService = resourceService;
    }

    @JacocoGenerated
    public BrageEntryEventConsumer() {
        this(S3Driver.defaultS3Client().build(), ResourceService.defaultService());
    }

    @Override
    public Publication handleRequest(S3Event s3Event, Context context) {
        return attempt(() -> parseBrageRecord(s3Event))
                   .map(publication -> pushAssociatedFilesToPersistedStorage(publication, s3Event))
                   .map(publication -> publicationWithCristinIdentifierAlreadyExists(publication)
                                           ? attemptToUpdateExistingPublication(publication, s3Event)
                                           : createNewPublication(publication, s3Event))
                   .orElseThrow(fail -> handleSavingError(fail, s3Event));
    }

    private static Publication injectAssociatedArtifacts(Publication publication, Publication existingPublication) {
        existingPublication.setAssociatedArtifacts(publication.getAssociatedArtifacts());
        return existingPublication;
    }

    private static Publication getOnlyElement(List<Publication> publications) {
        return Iterables.getOnlyElement(publications);
    }

    private boolean publicationWithCristinIdentifierAlreadyExists(Publication publication) {
        String cristinIdentifier = getCristinIdentifier(publication);
        if (nonNull(cristinIdentifier)) {
            publicationsByCristinIdentifier = getPublicationsByCristinIdentifier(cristinIdentifier);
            return !publicationsByCristinIdentifier.isEmpty();
        }
        return false;
    }

    private List<Publication> getPublicationsByCristinIdentifier(String cristinIdentifier) {
        return resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);
    }

    private Publication createNewPublication(Publication publication, S3Event s3Event) {
        return attempt(() -> publication)
                   .flatMap(this::persistInDatabase)
                   .map(pub -> storeHandleAndPublicationIdentifier(pub, s3Event))
                   .orElseThrow(fail -> handleSavingError(fail, s3Event));
    }

    private Publication attemptToUpdateExistingPublication(Publication publication, S3Event s3Event) {
        return attempt(() -> publicationsByCristinIdentifier)
                   .map(publications -> mergeTwoPublications(publication, getOnlyElement(publications), s3Event))
                   .orElseThrow();
    }

    private Publication mergeTwoPublications(Publication publication, Publication existingPublication,
                                             S3Event s3Event) {
        return attempt(() -> existingPublication)
                   .map(publicationToUpdate -> injectAssociatedArtifacts(publication, publicationToUpdate))
                   .map(resourceService::updatePublication)
                   .map(pub -> storeHandleAndPublicationIdentifier(pub, s3Event))
                   .orElseThrow();
    }

    private String getCristinIdentifier(Publication publication) {
        var cristinIdentifiers = getCristinIdentifiers(publication);
        if (cristinIdentifiers.isEmpty()) {
            return null;
        }
        return cristinIdentifiers.iterator().next();
    }

    private Set<String> getCristinIdentifiers(Publication publication) {
        return publication.getAdditionalIdentifiers()
                   .stream()
                   .filter(this::isCristinIdentifier)
                   .map(AdditionalIdentifier::getValue)
                   .collect(Collectors.toSet());
    }

    private boolean isCristinIdentifier(AdditionalIdentifier identifier) {
        return SOURCE_CRISTIN.equals(identifier.getSource());
    }

    private Publication storeHandleAndPublicationIdentifier(Publication publication, S3Event s3Event) {
        var handle = publication.getHandle();
        var fileUri = constructResourcehandleFileUri(s3Event, publication);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_ERROR_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), handle.toString())).orElseThrow();
        return publication;
    }

    private UriWrapper constructResourcehandleFileUri(S3Event s3Event, Publication publication) {
        var timestamp = timePath(s3Event);
        return UriWrapper.fromUri(HANDLE_REPORTS_PATH)
                   .addChild(timestamp)
                   .addChild(publication.getIdentifier().toString());
    }

    private RuntimeException handleSavingError(Failure<Publication> fail, S3Event s3Event) {

        String brageObjectKey = extractObjectKey(s3Event);
        String errorMessage = ERROR_SAVING_BRAGE_IMPORT + brageObjectKey;
        logger.error(errorMessage, fail.getException());
        saveReportToS3(fail, s3Event);
        return ExceptionMapper.castToCorrectRuntimeException(fail.getException());
    }

    private Try<Publication> persistInDatabase(Publication publication) {
        Try<Publication> attemptSave = tryPersistingInDatabase(publication);

        for (int efforts = 0; shouldTryAgain(attemptSave, efforts); efforts++) {
            attemptSave = tryPersistingInDatabase(publication);

            avoidCongestionInDatabase();
        }
        return attemptSave;
    }

    private void avoidCongestionInDatabase() {
        int sleepTime = spreadWriteRequests();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    // Randomize waiting time to avoiding creating parallel executions that perform retries in sync.
    private int spreadWriteRequests() {
        return RANDOM.nextInt(MAX_SLEEP_TIME);
    }

    private boolean shouldTryAgain(Try<Publication> attemptSave, int efforts) {
        return attemptSave.isFailure() && efforts < MAX_EFFORTS;
    }

    private Try<Publication> tryPersistingInDatabase(Publication publication) {
        return attempt(() -> createPublication(publication));
    }

    private Publication createPublication(Publication publication) {
        return resourceService.createPublicationFromImportedEntry(publication);
    }

    private Publication pushAssociatedFilesToPersistedStorage(Publication publication, S3Event s3Event) {
        var associatedArtifactMover = new AssociatedArtifactMover(s3Client, s3Event);
        associatedArtifactMover.pushAssociatedArtifactsToPersistedStorage(publication);
        return publication;
    }

    private void saveReportToS3(Failure<Publication> fail,
                                S3Event event) {
        var errorFileUri = constructErrorFileUri(event, fail.getException());
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_ERROR_BUCKET_NAME));
        var content = determineBestEventReference(event);
        var reportContent = ImportResult.reportFailure(content, fail.getException());
        attempt(() -> s3Driver.insertFile(errorFileUri.toS3bucketPath(), reportContent.toJsonString())).orElseThrow();
    }

    private String determineBestEventReference(S3Event event) {
        if (StringUtils.isNotEmpty(brageRecordFile)) {
            return brageRecordFile;
        } else {
            return extractObjectKey(event);
        }
    }

    private UriWrapper constructErrorFileUri(S3Event event,
                                             Exception exception) {
        var fileUri = UriWrapper.fromUri(extractObjectKey(event));
        var timestamp = timePath(event);
        return UriWrapper.fromUri(ERROR_BUCKET_PATH + PATH_SEPERATOR
                                  + timestamp + PATH_SEPERATOR + exception.getClass().getSimpleName() +
                                  PATH_SEPERATOR + fileUri.getLastPathElement());
    }

    private String timePath(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
    }

    private Publication parseBrageRecord(S3Event event)
        throws JsonProcessingException, InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var record = getBrageRecordFromS3(event);
        return convertBrageRecordToNvaPublication(record);
    }

    private Publication convertBrageRecordToNvaPublication(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return BrageNvaMapper.toNvaPublication(record);
    }

    private Record getBrageRecordFromS3(S3Event event) throws JsonProcessingException {
        brageRecordFile = readFileFromS3(event);
        return parseBrageRecordJson(brageRecordFile);
    }

    private Record parseBrageRecordJson(String brageRecordFile) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(brageRecordFile, Record.class);
    }

    private String readFileFromS3(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath());
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractObjectKey(s3Event)));
    }

    private String extractObjectKey(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getBucket().getName();
    }
}
