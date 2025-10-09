package no.sikt.nva.scopus;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConverter.API_HOST;
import static no.sikt.nva.scopus.ScopusConverter.RESOURCE_OWNER_SIKT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import jakarta.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.CristinConnection;
import no.sikt.nva.scopus.conversion.NvaCustomerConnection;
import no.sikt.nva.scopus.conversion.PiaConnection;
import no.sikt.nva.scopus.conversion.PublicationChannelConnection;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.conversion.files.TikaUtils;
import no.sikt.nva.scopus.update.ScopusUpdater;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class ScopusHandler implements RequestHandler<SQSEvent, ImportCandidate> {

    public static final String BACKEND_CLIENT_SECRET_NAME = new Environment().readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = new Environment().readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final int MAX_EFFORTS = 10;
    public static final String PATH_SEPERATOR = "/";
    public static final String SCOPUS_IMPORT_BUCKET = "SCOPUS_IMPORT_BUCKET";
    public static final String SCOPUS_XML_BUCKET = new Environment().readEnv("XML_BUCKET_NAME");
    public static final String SUCCESS_BUCKET_PATH = "SUCCESS";
    private static final String ERROR_SAVING_IMPORT_CANDIDATE = "Error saving import cadidate "
                                                                + "key: {} {}";
    private static final int MAX_SLEEP_TIME = 100;
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
    private static final String ERROR_BUCKET_PATH = "ERROR";
    public static final String URI_ATTRIBUTE = "uri";
    public static final String PUBLICATION = "publication";
    public static final String SCOPUS_IDENTIFIER = "scopusIdentifier";
    private final S3Client s3Client;
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;
    private final PublicationChannelConnection publicationChannelConnection;
    private final NvaCustomerConnection nvaCustomerConnection;
    private final ResourceService importCandidateService;
    private final ScopusUpdater scopusUpdater;
    private final ScopusFileConverter scopusFileConverter;
    private final SearchService searchService;
    private static final AtomicReference<AuthorizedBackendUriRetriever> authorizedBackendUriRetriever =
        new AtomicReference<>();

    @JacocoGenerated
    public ScopusHandler() {
        this(S3Driver.defaultS3Client().build(), defaultPiaConnection(), defaultCristinConnection(),
             new PublicationChannelConnection(getAuthorizedBackendUriRetriever()),
             new NvaCustomerConnection(getAuthorizedBackendUriRetriever()),
             ResourceService.defaultService(),
             new ScopusUpdater(ResourceService.defaultService(),
                               getAuthorizedBackendUriRetriever()),
             new ScopusFileConverter(defaultHttpClientWithRedirect(),
                                     S3Driver.defaultS3Client().build(),
                                     new TikaUtils()),
             SearchService.create(new UriRetriever(), ResourceService.defaultService(new Environment().readEnv("RESOURCES_TABLE_NAME"))));
    }

    public ScopusHandler(S3Client s3Client, PiaConnection piaConnection, CristinConnection cristinConnection,
                         PublicationChannelConnection publicationChannelConnection,
                         NvaCustomerConnection nvaCustomerConnection, ResourceService importCandidateService,
                         ScopusUpdater scopusUpdater, ScopusFileConverter scopusFileConverter, SearchService searchService) {
        this.s3Client = s3Client;
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
        this.publicationChannelConnection = publicationChannelConnection;
        this.nvaCustomerConnection = nvaCustomerConnection;
        this.importCandidateService = importCandidateService;
        this.scopusUpdater = scopusUpdater;
        this.scopusFileConverter = scopusFileConverter;
        this.searchService = searchService;
    }

    @Override
    public ImportCandidate handleRequest(SQSEvent event, Context context) {
        var message = event.getRecords().getFirst();
        var s3Uri = UriWrapper.fromUri(message.getMessageAttributes().get(URI_ATTRIBUTE).getStringValue()).getUri();
        return attempt(() -> createImportCandidate(s3Uri))
                   .map(this::updateExistingIfNeeded)
                   .map(this::injectImportedStatusWhenPublicationWithTheSameScopusIdentifierExists)
                   .flatMap(this::persistOrUpdateInDatabase)
                   .map(this::storeSuccessReport)
                   .orElse(fail -> handleSavingError(fail, s3Uri));
    }

    private ImportCandidate injectImportedStatusWhenPublicationWithTheSameScopusIdentifierExists(ImportCandidate importCandidate) {
        var scopusIdentifier = getScopusIdentifier(importCandidate);
        fetchPublicationsWithScopusIdentifier(scopusIdentifier)
            .ifPresent(resource -> setStatusImported(importCandidate, resource));
        return importCandidate;
    }

    private static void setStatusImported(ImportCandidate importCandidate, Resource resource) {
        importCandidate.setImportStatus(ImportStatusFactory.createImported(new Username(RESOURCE_OWNER_SIKT),
                                                                           createPublicationId(resource)));
    }

    private static URI createPublicationId(Resource resource) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION)
                   .addChild(resource.getIdentifier().toString())
                   .getUri();
    }

    private Optional<Resource> fetchPublicationsWithScopusIdentifier(String scopusIdentifier) {
        return searchService.searchPublicationsByParam(Map.of(SCOPUS_IDENTIFIER, scopusIdentifier)).stream()
                   .filter(resource -> hasScopusIdentifier(resource, scopusIdentifier))
                   .findFirst();
    }

    private boolean hasScopusIdentifier(Resource resource, String scopusIdentifier) {
        return resource.getAdditionalIdentifiers().stream()
                   .filter(ScopusIdentifier.class::isInstance)
                   .map(AdditionalIdentifierBase::value)
                   .anyMatch(scopusIdentifier::equals);
    }

    @JacocoGenerated
    private static HttpClient defaultHttpClientWithRedirect() {
        return HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
    }

    @JacocoGenerated
    private static PiaConnection defaultPiaConnection() {
        return new PiaConnection();
    }

    @JacocoGenerated
    private static CristinConnection defaultCristinConnection() {
        return new CristinConnection();
    }

    @JacocoGenerated
    private static AuthorizedBackendUriRetriever getAuthorizedBackendUriRetriever() {
        authorizedBackendUriRetriever.updateAndGet(ScopusHandler::getAuthorizedBackendUriRetriever);
        return authorizedBackendUriRetriever.get();
    }

    @JacocoGenerated
    private static AuthorizedBackendUriRetriever getAuthorizedBackendUriRetriever(
        AuthorizedBackendUriRetriever existing) {
        if (isNull(existing)) {
            return new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME);
        }
        return existing;
    }

    private static ImportResult<String> generateReportFromContent(Failure<ImportCandidate> fail, String content) {
        return ImportResult.reportFailure(content, fail.getException());
    }

    private ImportCandidate updateExistingIfNeeded(ImportCandidate importCandidate) throws NotFoundException {
        return scopusUpdater.updateImportCandidate(importCandidate);
    }

    private ImportCandidate storeSuccessReport(ImportCandidate importCandidate) {
        return attempt(this::getS3DriverForScopusImportBucket).map(
            s3Driver -> insertSucceededReportFile(importCandidate, s3Driver)).orElseThrow();
    }

    private ImportCandidate insertSucceededReportFile(ImportCandidate importCandidate, S3Driver s3Driver)
        throws IOException {
        s3Driver.insertFile(constructFileUri(importCandidate).toS3bucketPath(),
                            getScopusIdentifier(importCandidate));
        return importCandidate;
    }

    private S3Driver getS3DriverForScopusImportBucket() {
        return new S3Driver(s3Client, new Environment().readEnv(SCOPUS_IMPORT_BUCKET));
    }

    private UriWrapper constructFileUri(ImportCandidate importCandidate) {
        return UriWrapper.fromUri(SUCCESS_BUCKET_PATH)
                   .addChild(Instant.now().toString())
                   .addChild(importCandidate.getIdentifier().toString());
    }

    private String getScopusIdentifier(ImportCandidate importCandidate) {
        return importCandidate.getAdditionalIdentifiers()
                   .stream()
                   .filter(ScopusIdentifier.class::isInstance)
                   .map(ScopusIdentifier.class::cast)
                   .map(ScopusIdentifier::value)
                   .findFirst()
                   .orElse(null);
    }

    private ImportCandidate handleSavingError(Failure<ImportCandidate> fail, URI s3Uri) {
        loggError(s3Uri, fail);
        saveReportToS3(fail, s3Uri);
        return null;
    }

    private void loggError(URI s3Uri, Failure<ImportCandidate> fail) {
        logger.error(ERROR_SAVING_IMPORT_CANDIDATE, s3Uri, fail.getException());
    }

    private void saveReportToS3(Failure<ImportCandidate> fail, URI s3Uri) {
        attempt(() -> getContentToSave(s3Uri)).map(content -> generateReportFromContent(fail, content))
            .map(report -> insertReport(fail, s3Uri, report));
    }

    private URI insertReport(Failure<ImportCandidate> fail, URI s3Uri, ImportResult<String> report)
        throws IOException {
        return getS3DriverForScopusImportBucket().insertFile(
            constructErrorFileUri(s3Uri, fail.getException()).toS3bucketPath(), report.toJsonString());
    }

    private String getContentToSave(URI s3Uri) {
        return attempt(() -> readFile(s3Uri)).orElseThrow();
    }

    private UriWrapper constructErrorFileUri(URI s3Uri, Exception exception) {
        return UriWrapper.fromUri(ERROR_BUCKET_PATH
                                  + PATH_SEPERATOR
                                  + getWeekOfTheYear()
                                  + PATH_SEPERATOR
                                  + exception.getClass().getSimpleName()
                                  + PATH_SEPERATOR
                                  + UriWrapper.fromUri(s3Uri).getLastPathElement());
    }

    private static String getWeekOfTheYear() {
        var now = Instant.now().atZone(ZoneId.systemDefault());
        return "%s-%s".formatted(String.valueOf(now.get(WeekFields.of(Locale.getDefault()).weekOfYear())),
                                 String.valueOf(now.getYear()));
    }

    private Try<ImportCandidate> persistOrUpdateInDatabase(ImportCandidate importCandidate) throws BadRequestException {
        if (nonNull(importCandidate.getIdentifier())) {
            return Try.of(importCandidateService.updateImportCandidate(importCandidate));
        }
        return persistInDatabase(importCandidate);
    }

    private Try<ImportCandidate> persistInDatabase(ImportCandidate importCandidate) {
        var attemptSave = tryPersistingInDatabase(importCandidate);
        for (int efforts = 0; shouldTryAgain(attemptSave, efforts); efforts++) {
            attemptSave = tryPersistingInDatabase(importCandidate);
            avoidCongestionInDatabase();
        }
        return attemptSave;
    }

    @SuppressWarnings("PMD.DoNotUseThreads")
    private void avoidCongestionInDatabase() {
        int sleepTime = spreadWriteRequests();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }

    private int spreadWriteRequests() {
        return RANDOM.nextInt(MAX_SLEEP_TIME);
    }

    private boolean shouldTryAgain(Try<ImportCandidate> attemptSave, int efforts) {
        return attemptSave.isFailure() && efforts < MAX_EFFORTS;
    }

    private Try<ImportCandidate> tryPersistingInDatabase(ImportCandidate importCandidate) {
        return attempt(() -> createImportCandidate(importCandidate));
    }

    private ImportCandidate createImportCandidate(ImportCandidate importCandidate) {
        return importCandidateService.persistImportCandidate(importCandidate);
    }

    private ImportCandidate createImportCandidate(URI s3Uri) {
        return attempt(() -> readFile(s3Uri)).map(this::parseXmlFile)
                   .map(this::generateImportCandidate)
                   .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    private RuntimeException logErrorAndThrowException(Exception exception) {
        logger.error(exception.getMessage());
        return exception instanceof RuntimeException ? (RuntimeException) exception : new RuntimeException(exception);
    }

    private DocTp parseXmlFile(String file) {
        return JAXB.unmarshal(new StringReader(file), DocTp.class);
    }

    private ImportCandidate generateImportCandidate(DocTp docTp) {
        var scopusConverter = new ScopusConverter(docTp, piaConnection, cristinConnection,
                                                  publicationChannelConnection, nvaCustomerConnection,
                                                  scopusFileConverter);
        return scopusConverter.generateImportCandidate();
    }

    private String readFile(URI s3Uri) {
        var s3Driver = new S3Driver(s3Client, SCOPUS_XML_BUCKET);
        return s3Driver.getFile(UriWrapper.fromUri(s3Uri).toS3bucketPath());
    }
}
