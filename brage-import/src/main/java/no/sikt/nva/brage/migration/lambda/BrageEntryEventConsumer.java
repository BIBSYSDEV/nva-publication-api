package no.sikt.nva.brage.migration.lambda;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Iterables;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.mapper.BrageNvaMapper;
import no.sikt.nva.brage.migration.merger.AssociatedArtifactMover;
import no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger;
import no.sikt.nva.brage.migration.merger.DuplicatePublicationException;
import no.sikt.nva.brage.migration.merger.UnmappableCristinRecordException;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.NotFoundException;
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
public class BrageEntryEventConsumer implements RequestHandler<S3Event, Publication> {

    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final int MAX_EFFORTS = 10;
    public static final String SOURCE_CRISTIN = "Cristin";
    public static final String BRAGE_MIGRATION_REPORTS_BUCKET_NAME = "BRAGE_MIGRATION_ERROR_BUCKET_NAME";
    public static final String YYYY_MM_DD_HH_FORMAT = "yyyy-MM-dd:HH";
    public static final String ERROR_BUCKET_PATH = "ERROR";
    public static final String HANDLE_REPORTS_PATH = "HANDLE_REPORTS";
    public static final String PATH_SEPERATOR = "/";
    public static final String UPDATE_REPORTS_PATH = "UPDATE_REPORTS";
    public static final String CRISTIN_RECORD_EXCEPTION =
        "Cristin record has not been merged with existing publication: ";
    public static final String DUPLICATE_PUBLICATIONS_MESSAGE =
        "More than one publication with this cristin identifier already exists";
    private static final int MAX_SLEEP_TIME = 100;
    private static final int SINGLE_EXPECTED_RECORD = 0;
    private static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final String ERROR_SAVING_BRAGE_IMPORT = "Error saving brage import for record with object key: ";
    private static final Logger logger = LoggerFactory.getLogger(BrageEntryEventConsumer.class);
    private static final String RESOURCES_2 = "resources2";
    private static final String SEARCH = "search";
    private static final String DOI = "doi";
    private static final String APPLICATION_JSON = "application/json";
    private final S3Client s3Client;
    private final ResourceService resourceService;
    private String brageRecordFile;
    private List<Publication> publicationsToMerge;
    private final UriRetriever uriRetriever;
    private final String apiHost = new Environment().readEnv("API_HOST");

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
    public Publication handleRequest(S3Event s3Event, Context context) {
        return attempt(() -> parseBrageRecord(s3Event))
                   .map(publication -> pushAssociatedFilesToPersistedStorage(publication, s3Event))
                   .map(publication -> shouldMergePublications(publication)
                                           ? attemptToUpdateExistingPublication(publication, s3Event)
                                           : createNewPublication(publication, s3Event))
                   .orElse(fail -> handleSavingError(fail, s3Event));
    }

    private static Publication getOnlyElement(List<Publication> publications) {
        return attempt(() -> Iterables.getOnlyElement(publications))
                   .orElseThrow(fail -> new DuplicatePublicationException(
                       DUPLICATE_PUBLICATIONS_MESSAGE,
                       fail.getException()));
    }

    private boolean shouldMergePublications(Publication publication) throws NotFoundException {
        var cristinIdentifier = getCristinIdentifier(publication);
        if (nonNull(cristinIdentifier)) {
            publicationsToMerge = getPublicationsByCristinIdentifier(cristinIdentifier);
            return !publicationsToMerge.isEmpty();
        }

        return existingPublicationHasSameDoi(publication);
    }

    private boolean existingPublicationHasSameDoi(Publication publication)
        throws NotFoundException {
        var doi = publication.getEntityDescription().getReference().getDoi();

        if (nonNull(doi)) {
            var publicationsByDoi = searchForPublicationsByDoi(doi);
            if (publicationsByDoi.isEmpty()) {
                return false;
            }

            var firstPublicationHitByDoi = publicationsByDoi.get(0);

            var upToDateVersionOfPublication =
                resourceService.getPublicationByIdentifier(firstPublicationHitByDoi.getIdentifier());
            publicationsToMerge = List.of(upToDateVersionOfPublication);

            return true;
        }
        return false;
    }

    private List<Publication> searchForPublicationsByDoi(URI doi) {
        var searchUri = constructSearchUri(doi);
        return getResponseBody(searchUri)
                .map(this::toResponse)
                .map(SearchResourceApiResponse::hits)
                .orElse(List.of());
    }

    private URI constructSearchUri(URI doi) {
        return UriWrapper.fromHost(apiHost)
                   .addChild(SEARCH)
                   .addChild(RESOURCES_2)
                   .addQueryParameter(DOI, doi.toString())
                   .getUri();
    }

    private Optional<String> getResponseBody(URI uri) {
        return uriRetriever.getRawContent(uri, APPLICATION_JSON);
    }

    private SearchResourceApiResponse toResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, SearchResourceApiResponse.class))
                   .orElseThrow();
    }

    private List<Publication> getPublicationsByCristinIdentifier(String cristinIdentifier) {
        return resourceService.getPublicationsByCristinIdentifier(cristinIdentifier);
    }

    private Publication createNewPublication(Publication publication, S3Event s3Event) {
        return isEmptyCristinRecord(publication)
                   ? unableToMergeCristinRecordException(publication, s3Event)
                   : persistPublication(publication, s3Event);
    }

    private Publication persistPublication(Publication publication, S3Event s3Event) {
        return attempt(() -> publication)
                   .flatMap(this::persistInDatabase)
                   .map(pub -> storeHandleAndPublicationIdentifier(pub, s3Event))
                   .orElse(fail -> handleSavingError(fail, s3Event));
    }

    private Publication unableToMergeCristinRecordException(Publication publication, S3Event s3Event) {
        handleSavingError(new Failure<>(
            new UnmappableCristinRecordException(CRISTIN_RECORD_EXCEPTION
                                                 + getCristinIdentifier(publication))), s3Event);
        return null;
    }

    private boolean isEmptyCristinRecord(Publication publication) {
        return isNull(publication.getEntityDescription().getReference().getPublicationInstance())
               & isNull(publication.getEntityDescription().getReference().getPublicationContext())
               & nonNull(getCristinIdentifier(publication));
    }

    private Publication attemptToUpdateExistingPublication(Publication publication, S3Event s3Event) {
        return attempt(() -> publicationsToMerge)
                   .map(publications -> mergeTwoPublications(publication, getOnlyElement(publications), s3Event))
                   .orElseThrow();
    }

    private void storePublicationBeforeUpdate(Publication publication, S3Event s3Event) {
        var fileUri = updateResourceFilePath(publication, s3Event);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), publication.toString())).orElseThrow();
    }

    private UriWrapper updateResourceFilePath(Publication publication, S3Event s3Event) {
        return UriWrapper.fromUri(UPDATE_REPORTS_PATH)
                   .addChild(extractInstitutionName(s3Event))
                   .addChild(timePath(s3Event))
                   .addChild(publication.getIdentifier().toString());
    }

    private Publication mergeTwoPublications(Publication publication, Publication existingPublication,
                                             S3Event s3Event) {
        storePublicationBeforeUpdate(existingPublication, s3Event);
        return attempt(() -> updatedPublication(publication, existingPublication))
                   .map(resourceService::updatePublication)
                   .map(updatedPublication -> storeHandleAndPublicationIdentifier(updatedPublication, s3Event))
                   .orElseThrow();
    }

    private Publication updatedPublication(Publication publication, Publication existingPublication) {
        var cristinImportPublicationMerger = new CristinImportPublicationMerger(existingPublication, publication);
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

    private Publication storeHandleAndPublicationIdentifier(Publication publication, S3Event s3Event) {
        var handle = publication.getHandle();
        var fileUri = constructResourcehandleFileUri(s3Event, publication);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), handle.toString())).orElseThrow();
        return publication;
    }

    private UriWrapper constructResourcehandleFileUri(S3Event s3Event, Publication publication) {
        var timestamp = timePath(s3Event);
        return UriWrapper.fromUri(HANDLE_REPORTS_PATH)
                   .addChild(extractInstitutionName(s3Event))
                   .addChild(timestamp)
                   .addChild(publication.getIdentifier().toString());
    }

    private Publication handleSavingError(Failure<Publication> fail, S3Event s3Event) {
        String brageObjectKey = extractObjectKey(s3Event);
        String errorMessage = ERROR_SAVING_BRAGE_IMPORT + brageObjectKey;
        logger.error(errorMessage, fail.getException());
        saveReportToS3(fail, s3Event);
        return null;
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
            Thread.currentThread().interrupt();
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
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
    }

    private Publication parseBrageRecord(S3Event event)
        throws JsonProcessingException, InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var brageRecord = getBrageRecordFromS3(event);
        return convertBrageRecordToNvaPublication(brageRecord);
    }

    private Publication convertBrageRecordToNvaPublication(Record brageRecord)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return BrageNvaMapper.toNvaPublication(brageRecord);
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

    private String extractInstitutionName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey().split("/")[0];
    }

    private String extractObjectKey(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getBucket().getName();
    }
}
