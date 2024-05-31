package no.sikt.nva.brage.migration.lambda;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Iterables;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.mapper.BrageNvaMapper;
import no.sikt.nva.brage.migration.merger.AssociatedArtifactMover;
import no.sikt.nva.brage.migration.merger.BrageMergingReport;
import no.sikt.nva.brage.migration.merger.CristinImportPublicationMerger;
import no.sikt.nva.brage.migration.merger.DiscardedFilesReport;
import no.sikt.nva.brage.migration.merger.DuplicatePublicationException;
import no.sikt.nva.brage.migration.merger.UnmappableCristinRecordException;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.ResourceWithId;
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
    private static final String RESOURCES = "resources";
    private static final String SEARCH = "search";
    private static final String DOI = "doi";
    private static final String APPLICATION_JSON = "application/json";
    public static final String TITLE = "title";
    public static final String CONTEXT_TYPE = "contextType";
    public static final String AGGREGATION = "aggregation";
    public static final String NONE = "none";
    public static final String ISBN = "isbn";
    private final S3Client s3Client;
    private final ResourceService resourceService;
    private String brageRecordFile;
    private List<Publication> publicationsToMerge;
    private MergeSource source;
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
            publicationsToMerge = resourceService.getPublicationsByCristinIdentifier(cristinIdentifier).stream()
                                      .filter(item -> PublicationComparator.publicationsMatch(item, publication))
                                      .toList();
            boolean shouldMerge = !publicationsToMerge.isEmpty();
            source = shouldMerge ? MergeSource.CRISTIN : MergeSource.NOT_RELEVANT;
            return shouldMerge;
        }
        if (hasDoi(publication)) {
            var shouldMerge= existingPublicationHasSameDoi(publication);
            source = shouldMerge ? MergeSource.DOI : MergeSource.NOT_RELEVANT;
            return shouldMerge;
        }
        if (hasIsbn(publication)) {
            var shouldMerge = existingPublicationHasSameIsbn(publication);
            source = shouldMerge ? MergeSource.ISBN : MergeSource.NOT_RELEVANT;
            return shouldMerge;
        } else {
            var shouldMerge = existingPublicationHasSamePublicationContent(publication);
            source = shouldMerge ? MergeSource.SEARCH : MergeSource.NOT_RELEVANT;
            return shouldMerge;
        }
    }

    private boolean existingPublicationHasSameIsbn(Publication publication) {
        var isbnList = ((Book) publication.getEntityDescription().getReference().getPublicationContext()).getIsbnList();
        publicationsToMerge = isbnList.stream()
                                  .map(isbn -> fetchPublicationsByParam(ISBN, isbn))
                                  .flatMap(List::stream)
                                  .filter(item -> PublicationComparator.publicationsMatch(item, publication))
                                  .collect(Collectors.toList());
        return !publicationsToMerge.isEmpty();
    }

    private List<Publication> fetchPublicationsByParam(String searchParam, String value) {
        var uri = searchPublicationByParamUri(searchParam, value);
        return uriRetriever.getRawContent(uri, APPLICATION_JSON)
                     .map(this::toResponse)
                     .map(SearchResourceApiResponse::hits)
                     .stream()
                     .flatMap(List::stream)
                     .map(ResourceWithId::getIdentifier)
                     .map(this::getPublicationByIdentifier)
                     .toList();
    }

    private URI searchPublicationByParamUri(String searchParam, String value) {
        return UriWrapper.fromHost(apiHost)
                   .addChild(SEARCH)
                   .addChild(RESOURCES)
                   .addQueryParameter(searchParam, value)
                   .addQueryParameter(AGGREGATION, NONE)
                   .getUri();
    }

    private boolean hasIsbn(Publication publication) {
        if (publication.getEntityDescription().getReference().getPublicationContext() instanceof Book book) {
            return !book.getIsbnList().isEmpty();
        } else {
            return false;
        }
    }

    private boolean existingPublicationHasSamePublicationContent(Publication publication) {
        publicationsToMerge = searchForPublicationsByTypeAndTitle(publication);
        return !publicationsToMerge.isEmpty();

    }

    private List<Publication> searchForPublicationsByTypeAndTitle(Publication publication) {
        var response = fetchResponse(searchByTypeAndTitleUri(publication));
        return response.map(this::toResponse)
                   .filter(SearchResourceApiResponse::containsSingleHit)
                   .map(SearchResourceApiResponse::hits)
                   .orElse(List.of())
                   .stream()
                   .map(ResourceWithId::getIdentifier)
                   .map(this::getPublicationByIdentifier)
                   .filter(item -> PublicationComparator.publicationsMatch(item, publication))
                   .collect(Collectors.toList()).reversed();
    }

    private Publication getPublicationByIdentifier(SortableIdentifier identifier) {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifier)).orElseThrow();
    }

    private Optional<String> fetchResponse(URI uri) {
        var response = uriRetriever.fetchResponse(uri);
        if (isNotHttpOk(response)) {
            logger.info("Search-api responded with statusCode: {} for request: {}", response.statusCode(), uri);
            return Optional.empty();
        } else {
            return Optional.ofNullable(response.body());
        }
    }

    private static boolean isNotHttpOk(HttpResponse<String> response) {
        return response.statusCode() != HTTP_OK;
    }

    private URI searchByTypeAndTitleUri(Publication publication) {
        return UriWrapper.fromHost(apiHost)
                   .addChild(SEARCH)
                   .addChild(RESOURCES)
                   .addQueryParameter(TITLE, getMainTitle(publication))
                   .addQueryParameter(CONTEXT_TYPE, getInstanceType(publication))
                   .addQueryParameter(AGGREGATION, NONE)
                   .getUri();
    }

    private static String getMainTitle(Publication publication) {
        return publication.getEntityDescription().getMainTitle();
    }

    private static String getInstanceType(Publication publication) {
        return publication.getEntityDescription().getReference().getPublicationInstance().getInstanceType();
    }

    private static boolean hasDoi(Publication publication) {
        return nonNull(publication.getEntityDescription().getReference().getDoi());
    }

    private boolean existingPublicationHasSameDoi(Publication publication)
        throws NotFoundException {
        var doi = publication.getEntityDescription().getReference().getDoi();

        if (nonNull(doi)) {
            var publicationsByDoi = fetchPublicationsByParam(DOI, doi.toString()).stream()
                                        .filter(item -> PublicationComparator.publicationsMatch(item, publication))
                                        .toList();
            if (publicationsByDoi.isEmpty()) {
                return false;
            }

            var firstPublicationHitByDoi = publicationsByDoi.getFirst();

            var upToDateVersionOfPublication =
                resourceService.getPublicationByIdentifier(firstPublicationHitByDoi.getIdentifier());
            publicationsToMerge = List.of(upToDateVersionOfPublication);

            return true;
        }
        return false;
    }

    private SearchResourceApiResponse toResponse(String response) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(response, SearchResourceApiResponse.class))
                   .orElseThrow();
    }

    private Publication createNewPublication(Publication publication, S3Event s3Event) {
        return isEmptyCristinRecord(publication)
                   ? unableToMergeCristinRecordException(publication, s3Event)
                   : persistPublication(publication, s3Event);
    }

    private Publication persistPublication(Publication publication, S3Event s3Event) {
        return attempt(() -> publication)
                   .flatMap(this::persistInDatabase)
                   .map(pub -> persistReports(s3Event, pub))
                   .orElse(fail -> handleSavingError(fail, s3Event));
    }

    private Publication persistReports(S3Event s3Event, Publication publication) {
        persistPartOfReport(publication, s3Client, s3Event);
        persistHandleReport(publication, publication.getHandle(), s3Event, HANDLE_REPORTS_PATH);
        return publication;
    }

    private void persistPartOfReport(Publication publication, S3Client s3Client, S3Event s3Event) {
        var record = attempt(() -> JsonUtils.dtoObjectMapper.readValue(brageRecordFile, Record.class)).orElseThrow();
        if (record.hasParentPublication()) {
            new PartOfReport(publication, record).persist(s3Client, timePath(s3Event));
        }
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

    private void persistUpdateReport(S3Event s3Event,
                                     BrageMergingReport brageMergingReport,
                                     Publication brageConversion) {
        var fileUri = updateResourceFilePath(brageMergingReport.newImage(), s3Event,
                                             brageConversion.getHandle().getPath());
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), brageMergingReport.toString())).orElseThrow();
    }

    private UriWrapper updateResourceFilePath(Publication publication, S3Event s3Event, String brageHandle) {
        return UriWrapper.fromUri(UPDATE_REPORTS_PATH)
                   .addChild(source.name())
                   .addChild(extractInstitutionName(s3Event))
                   .addChild(timePath(s3Event))
                   .addChild(brageHandle)
                   .addChild(publication.getIdentifier().toString());
    }

    private Publication mergeTwoPublications(Publication publication, Publication existingPublication,
                                             S3Event s3Event) {
        return attempt(() -> updatedPublication(publication, existingPublication))
                   .map(publicationForUpdate -> persistInDatabaseAndCreateMergeReport(publicationForUpdate,
                                                                                      existingPublication))
                   .map(mergeReport -> persistMergeReports(mergeReport, s3Event, publication))
                   .orElseThrow();
    }

    private Publication persistMergeReports(BrageMergingReport mergingReport, S3Event s3Event,
                                            Publication brageConversion) {
        persistUpdateReport(s3Event, mergingReport, brageConversion);
        persistHandleReport(mergingReport.newImage(),
                            brageConversion.getHandle(),
                            s3Event,
                            UPDATED_PUBLICATIONS_REPORTS_PATH);
        persistDiscardedFilesReport(mergingReport, brageConversion, s3Event);
        return mergingReport.newImage();
    }

    private void persistDiscardedFilesReport(BrageMergingReport mergingReport,
                                             Publication brageConversion,
                                             S3Event s3Event) {

        var discardedFilesReport = DiscardedFilesReport.fromBrageMergeReport(mergingReport, brageConversion);
        var fileUri = discardedFilesReportUri(mergingReport.newImage(), s3Event,
                                              brageConversion.getHandle().getPath());
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

    private Publication updatedPublication(Publication publication, Publication existingPublication)
        throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
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

    private void persistHandleReport(Publication publication,
                                            URI brageHandle,
                                            S3Event s3Event,
                                            String destinationFolder) {
        var fileUri = constructResourceHandleFileUri(s3Event, publication, destinationFolder, brageHandle);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), brageHandle.toString())).orElseThrow();
    }

    private UriWrapper constructResourceHandleFileUri(S3Event s3Event,
                                                      Publication publication,
                                                      String destinationFolder,
                                                      URI brageHandle) {
        var timestamp = timePath(s3Event);
        return UriWrapper.fromUri(destinationFolder)
                   .addChild(extractInstitutionName(s3Event))
                   .addChild(timestamp)
                   .addChild(brageHandle.getPath())
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
        return event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
    }

    private Publication parseBrageRecord(S3Event event)
        throws JsonProcessingException, InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var brageRecord = getBrageRecordFromS3(event);
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
        return event.getRecords().getFirst().getS3().getObject().getKey().split("/")[0];
    }

    private String extractObjectKey(S3Event event) {
        return event.getRecords().getFirst().getS3().getObject().getKey();
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().getFirst().getS3().getBucket().getName();
    }
}
