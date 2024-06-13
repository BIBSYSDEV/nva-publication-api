package no.sikt.nva.brage.migration.lambda;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.mapper.BrageNvaMapper;
import no.sikt.nva.brage.migration.merger.AssociatedArtifactMover;
import no.sikt.nva.brage.migration.merger.BrageMergingReport;
import no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger;
import no.sikt.nva.brage.migration.merger.DiscardedFilesReport;
import no.sikt.nva.brage.migration.merger.UnmappableCristinRecordException;
import no.sikt.nva.brage.migration.merger.findexistingpublication.FindExistingPublicationServiceImpl;
import no.sikt.nva.brage.migration.model.PublicationForUpdate;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.publication.exception.GatewayTimeoutException;
import no.unit.nva.publication.external.services.UriRetriever;
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

@SuppressWarnings("PMD.GodClass")
public class BrageEntryEventConsumer implements RequestHandler<S3Event, PublicationRepresentation> {

    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final int MAX_EFFORTS = 10;
    public static final String SOURCE_CRISTIN = "Cristin";
    public static final String BRAGE_MIGRATION_REPORTS_BUCKET_NAME = "BRAGE_MIGRATION_ERROR_BUCKET_NAME";
    public static final String YYYY_MM_DD_HH_FORMAT = "yyyy-MM-dd:HH";
    public static final String ERROR_BUCKET_PATH = "ERROR";
    public static final String HANDLE_REPORTS_PATH = "HANDLE_REPORTS";
    public static final String UPDATED_PUBLICATIONS_REPORTS_PATH = "UPDATED_PUBLICATIONS_HANDLE_REPORTS";
    public static final String PATH_SEPERATOR = "/";
    public static final String UPDATE_REPORTS_PATH = "UPDATE_REPORTS";
    public static final String CRISTIN_RECORD_EXCEPTION =
        "Cristin record has not been merged with existing publication: ";
    public static final String DUPLICATE_PUBLICATIONS_MESSAGE =
        "More than one publication with this cristin identifier already exists";
    private static final int MAX_SLEEP_TIME = 100;
    private static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final String ERROR_SAVING_BRAGE_IMPORT = "Error saving brage import for record with object key: ";
    private static final Logger logger = LoggerFactory.getLogger(BrageEntryEventConsumer.class);
    private final S3Client s3Client;
    private final ResourceService resourceService;
    private final UriRetriever uriRetriever;
    private final String apiHost = new Environment().readEnv("API_HOST");
    private String brageRecordFile;

    public BrageEntryEventConsumer(S3Client s3Client, ResourceService resourceService, UriRetriever uriRetriever) {
        this.s3Client = s3Client;
        this.resourceService = resourceService;
        this.uriRetriever = uriRetriever;
    }

    @JacocoGenerated
    public BrageEntryEventConsumer() {
        this(S3Driver.defaultS3Client().build(), ResourceService.defaultService(), new UriRetriever());
    }

    @Override
    public PublicationRepresentation handleRequest(S3Event s3Event, Context context) {
        return attempt(() -> parseBrageRecord(s3Event))
                   .map(publicationRepresentation -> pushAssociatedFilesToPersistedStorage(publicationRepresentation,
                                                                                           s3Event))
                   .map(publicationRepresentation -> persistMetadataChange(s3Event, publicationRepresentation))
                   .orElse(fail -> handleSavingError(fail, s3Event));
    }

    private PublicationRepresentation persistMetadataChange(S3Event s3Event,
                                                            PublicationRepresentation publicationRepresentation) {
        var publicationForUpdateOptional = findExistingPublication(publicationRepresentation);
        return publicationForUpdateOptional.map(
                publicationForUpdate -> attemptToUpdateExistingPublication(publicationRepresentation,
                                                                           s3Event,
                                                                           publicationForUpdate))
                   .orElseGet(() -> createNewPublication(publicationRepresentation, s3Event));
    }

    private Optional<PublicationForUpdate> findExistingPublication(
        PublicationRepresentation publicationRepresentation) {
        var publicationFinderService = new FindExistingPublicationServiceImpl(resourceService, uriRetriever, apiHost);
        return publicationFinderService.findExistingPublication(publicationRepresentation);
    }



    private PublicationRepresentation createNewPublication(PublicationRepresentation publicationRepresentation,
                                                           S3Event s3Event) {
        return isEmptyCristinRecord(publicationRepresentation.publication())
                   ? unableToMergeCristinRecordException(publicationRepresentation, s3Event)
                   : persistPublication(publicationRepresentation, s3Event);
    }

    private PublicationRepresentation persistPublication(PublicationRepresentation publicationRepresentation,
                                                         S3Event s3Event) {
        return attempt(() -> publicationRepresentation)
                   .flatMap(this::persistInDatabase)
                   .map(pub -> persistReports(s3Event, pub))
                   .orElse(fail -> handleSavingError(fail, s3Event));
    }

    private PublicationRepresentation persistReports(S3Event s3Event,
                                                     PublicationRepresentation publicationRepresentation) {
        persistPartOfReport(publicationRepresentation.publication(), s3Client, s3Event);
        persistHandleReport(publicationRepresentation.publication().getIdentifier(),
                            publicationRepresentation.brageRecord().getId(), s3Event,
                            HANDLE_REPORTS_PATH);
        return publicationRepresentation;
    }

    private void persistPartOfReport(Publication publication, S3Client s3Client, S3Event s3Event) {
        var record = attempt(() -> JsonUtils.dtoObjectMapper.readValue(brageRecordFile, Record.class)).orElseThrow();
        if (record.hasParentPublication()) {
            new PartOfReport(publication, record).persist(s3Client, timePath(s3Event));
        }
    }

    private PublicationRepresentation unableToMergeCristinRecordException(
        PublicationRepresentation publicationRepresentation,
        S3Event s3Event) {
        handleSavingError(new Failure<>(
                              new UnmappableCristinRecordException(CRISTIN_RECORD_EXCEPTION
                                                                   + getCristinIdentifier(publicationRepresentation.publication()))),
                          s3Event);
        return null;
    }

    private boolean isEmptyCristinRecord(Publication publication) {
        return isNull(publication.getEntityDescription().getReference().getPublicationInstance())
               && isNull(publication.getEntityDescription().getReference().getPublicationContext())
               && nonNull(getCristinIdentifier(publication));
    }

    private PublicationRepresentation attemptToUpdateExistingPublication(
        PublicationRepresentation publicationRepresentation,
        S3Event s3Event,
        PublicationForUpdate publicationForUpdate) {
        return attempt(() -> mergeTwoPublications(publicationRepresentation, publicationForUpdate
            , s3Event))
                   .map(publication -> new PublicationRepresentation(publicationRepresentation.brageRecord(),
                                                                     publication))
                   .orElseThrow();
    }

    private void persistUpdateReport(S3Event s3Event,
                                     BrageMergingReport brageMergingReport,
                                     PublicationRepresentation brageConversion,
                                     MergeSource mergeSource) {
        var fileUri = updateResourceFilePath(brageMergingReport.newImage(), s3Event,
                                             brageConversion.brageRecord().getId().getPath(), mergeSource);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), brageMergingReport.toString())).orElseThrow();
    }

    private UriWrapper updateResourceFilePath(Publication publication, S3Event s3Event, String brageHandle,
                                              MergeSource mergeSource) {
        return UriWrapper.fromUri(UPDATE_REPORTS_PATH)
                   .addChild(mergeSource.name())
                   .addChild(extractInstitutionName(s3Event))
                   .addChild(timePath(s3Event))
                   .addChild(brageHandle)
                   .addChild(publication.getIdentifier().toString());
    }

    private Publication mergeTwoPublications(PublicationRepresentation publicationRepresentation,
                                             PublicationForUpdate existingPublication,
                                             S3Event s3Event) {
        return attempt(() -> updatedPublication(publicationRepresentation, existingPublication.existingPublication()))
                   .map(publicationForUpdate -> persistInDatabaseAndCreateMergeReport(publicationForUpdate,
                                                                                      existingPublication.existingPublication()))
                   .map(mergeReport -> persistMergeReports(mergeReport, s3Event, publicationRepresentation,
                                                           existingPublication))
                   .orElseThrow();
    }

    private Publication persistMergeReports(BrageMergingReport mergingReport,
                                            S3Event s3Event,
                                            PublicationRepresentation brageConversion,
                                            PublicationForUpdate existingPublication) {
        persistUpdateReport(s3Event, mergingReport, brageConversion, existingPublication.source());
        persistHandleReport(mergingReport.newImage().getIdentifier(),
                            brageConversion.brageRecord().getId(),
                            s3Event,
                            UPDATED_PUBLICATIONS_REPORTS_PATH);
        persistDiscardedFilesReport(mergingReport, brageConversion, s3Event);
        return mergingReport.newImage();
    }

    private void persistDiscardedFilesReport(BrageMergingReport mergingReport,
                                             PublicationRepresentation brageConversion,
                                             S3Event s3Event) {

        var discardedFilesReport = DiscardedFilesReport.fromBrageMergeReport(mergingReport, brageConversion);
        var fileUri = discardedFilesReportUri(mergingReport.newImage(), s3Event,
                                              brageConversion.brageRecord().getId().getPath());
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), discardedFilesReport.toString())).orElseThrow();
    }

    private UriWrapper discardedFilesReportUri(Publication publication, S3Event s3Event, String brageHandle) {
        return UriWrapper.fromUri("DISCARDED_CONTENT_FILES")
                   .addChild(extractInstitutionName(s3Event))
                   .addChild(timePath(s3Event))
                   .addChild(brageHandle)
                   .addChild(publication.getIdentifier().toString());
    }

    private BrageMergingReport persistInDatabaseAndCreateMergeReport(Publication publicationForUpdate,
                                                                     Publication existinPublication) {
        var newImage = resourceService.updatePublication(publicationForUpdate);
        return new BrageMergingReport(existinPublication, newImage);
    }

    private Publication updatedPublication(PublicationRepresentation publicationRepresentation,
                                           Publication existingPublication)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException, InvalidIssnException {
        var cristinImportPublicationMerger = new CristinImportPublicationMerger(existingPublication,
            publicationRepresentation);
        return cristinImportPublicationMerger.mergePublications();
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
        return SOURCE_CRISTIN.equals(identifier.getSourceName());
    }

    private void persistHandleReport(SortableIdentifier nvaPublicationIdentifier,
                                     URI brageHandle,
                                     S3Event s3Event,
                                     String destinationFolder) {
        var fileUri = constructResourceHandleFileUri(s3Event,
                                                     nvaPublicationIdentifier,
                                                     destinationFolder,
                                                     brageHandle);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), brageHandle.toString())).orElseThrow();
    }

    private UriWrapper constructResourceHandleFileUri(S3Event s3Event,
                                                      SortableIdentifier publicationIdentifier,
                                                      String destinationFolder,
                                                      URI brageHandle) {
        var timestamp = timePath(s3Event);
        return UriWrapper.fromUri(destinationFolder)
                   .addChild(extractInstitutionName(s3Event))
                   .addChild(timestamp)
                   .addChild(brageHandle.getPath())
                   .addChild(publicationIdentifier.toString());
    }

    private PublicationRepresentation handleSavingError(Failure<PublicationRepresentation> fail, S3Event s3Event) {
        String brageObjectKey = extractObjectKey(s3Event);
        String errorMessage = ERROR_SAVING_BRAGE_IMPORT + brageObjectKey;
        logger.error(errorMessage, fail.getException());
        saveReportToS3(fail, s3Event);
        return null;
    }

    private Try<PublicationRepresentation> persistInDatabase(PublicationRepresentation publicationRepresentation) {
        Try<PublicationRepresentation> attemptSave = tryPersistingInDatabase(publicationRepresentation);

        for (int efforts = 0; shouldTryAgain(attemptSave, efforts); efforts++) {
            attemptSave = tryPersistingInDatabase(publicationRepresentation);

            avoidCongestionInDatabase();
        }
        return attemptSave;
    }

    private void avoidCongestionInDatabase() {
        int sleepTime = spreadWriteRequests();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }

    // Randomize waiting time to avoiding creating parallel executions that perform retries in sync.
    private int spreadWriteRequests() {
        return RANDOM.nextInt(MAX_SLEEP_TIME);
    }

    private boolean shouldTryAgain(Try<PublicationRepresentation> attemptSave, int efforts) {
        return attemptSave.isFailure() && efforts < MAX_EFFORTS;
    }

    private Try<PublicationRepresentation> tryPersistingInDatabase(
        PublicationRepresentation publicationRepresentation) {
        return attempt(() -> createPublication(publicationRepresentation));
    }

    private PublicationRepresentation createPublication(PublicationRepresentation publicationRepresentation)
        throws GatewayTimeoutException {
        var updatedPublication =
            resourceService.createPublicationFromImportedEntry(publicationRepresentation.publication());
        return new PublicationRepresentation(publicationRepresentation.brageRecord(), updatedPublication);
    }

    private PublicationRepresentation pushAssociatedFilesToPersistedStorage(
        PublicationRepresentation publicationRepresentation,
        S3Event s3Event) {
        var associatedArtifactMover = new AssociatedArtifactMover(s3Client, s3Event);
        associatedArtifactMover.pushAssociatedArtifactsToPersistedStorage(publicationRepresentation.publication());
        return publicationRepresentation;
    }

    private void saveReportToS3(Failure<PublicationRepresentation> fail,
                                S3Event event) {
        var errorFileUri = constructErrorFileUri(event, fail.getException());
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        var content = attempt(() -> JsonUtils.dtoObjectMapper.readTree(determineBestEventReference(event)))
                          .orElseThrow();
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
        return UriWrapper.fromUri(ERROR_BUCKET_PATH)
                   .addChild(extractInstitutionName(event))
                   .addChild(timePath(event))
                   .addChild(exception.getClass().getSimpleName())
                   .addChild(UriWrapper.fromUri(extractObjectKey(event)).getLastPathElement());
    }

    private String timePath(S3Event event) {
        return event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
    }

    private PublicationRepresentation parseBrageRecord(S3Event event)
        throws JsonProcessingException, InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var brageRecord = getBrageRecordFromS3(event);
        var nvaPublication = BrageNvaMapper.toNvaPublication(brageRecord);
        return new PublicationRepresentation(brageRecord, nvaPublication);
    }

    private Record getBrageRecordFromS3(S3Event event) throws JsonProcessingException {
        brageRecordFile = readFileFromS3(event);
        return parseBrageRecordJson(readFileFromS3(event));
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

    private String extractInstitutionName(S3Event event) {
        return event.getRecords().getFirst().getS3().getObject().getKey().split("/")[0];
    }

    private String extractObjectKey(S3Event event) {
        return event.getRecords().getFirst().getS3().getObject().getKey();
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().getFirst().getS3().getBucket().getName();
    }
}
