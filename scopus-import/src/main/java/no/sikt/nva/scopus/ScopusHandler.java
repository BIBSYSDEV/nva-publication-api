package no.sikt.nva.scopus;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.Random;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.CristinConnection;
import no.sikt.nva.scopus.conversion.NvaCustomerConnection;
import no.sikt.nva.scopus.conversion.PiaConnection;
import no.sikt.nva.scopus.conversion.PublicationChannelConnection;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.conversion.files.TikaUtils;
import no.sikt.nva.scopus.exception.ExceptionMapper;
import no.sikt.nva.scopus.update.ScopusUpdater;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.model.Publication;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
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

@SuppressWarnings("PMD.GodClass")
public class ScopusHandler implements RequestHandler<S3Event, Publication> {

    public static final String BACKEND_CLIENT_SECRET_NAME = new Environment().readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = new Environment().readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String YYYY_MM_DD_HH_FORMAT = "yyyy-MM-dd:HH";
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final int MAX_EFFORTS = 10;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    public static final String PATH_SEPERATOR = "/";
    public static final String SCOPUS_IMPORT_BUCKET = "SCOPUS_IMPORT_BUCKET";
    public static final String SUCCESS_BUCKET_PATH = "SUCCESS";
    private static final String ERROR_SAVING_SCOPUS_PUBLICATION = "Error saving imported scopus publication object "
                                                                  + "key: {} {}";
    private static final int MAX_SLEEP_TIME = 100;
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
    private static final String ERROR_BUCKET_PATH = "ERROR";
    private final S3Client s3Client;
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;
    private final PublicationChannelConnection publicationChannelConnection;
    private final NvaCustomerConnection nvaCustomerConnection;
    private final ResourceService resourceService;
    private final ScopusUpdater scopusUpdater;
    private final ScopusFileConverter scopusFileConverter;

    @JacocoGenerated
    public ScopusHandler() {
        this(S3Driver.defaultS3Client().build(), defaultPiaConnection(), defaultCristinConnection(),
             new PublicationChannelConnection(new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL,
                                                                                BACKEND_CLIENT_SECRET_NAME)),
             new NvaCustomerConnection(new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL,
                                                                         BACKEND_CLIENT_SECRET_NAME)),
             ResourceService.defaultService(),
             new ScopusUpdater(ResourceService.defaultService(),
                               new no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever(
                                   BACKEND_CLIENT_AUTH_URL, BACKEND_CLIENT_SECRET_NAME)),
             new ScopusFileConverter(defaultHttpClientWithRedirect(),
                                     S3Driver.defaultS3Client().build(),
                                     new TikaUtils()));
    }

    public ScopusHandler(S3Client s3Client, PiaConnection piaConnection, CristinConnection cristinConnection,
                         PublicationChannelConnection publicationChannelConnection,
                         NvaCustomerConnection nvaCustomerConnection, ResourceService resourceService,
                         ScopusUpdater scopusUpdater, ScopusFileConverter scopusFileConverter) {
        this.s3Client = s3Client;
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
        this.publicationChannelConnection = publicationChannelConnection;
        this.nvaCustomerConnection = nvaCustomerConnection;
        this.resourceService = resourceService;
        this.scopusUpdater = scopusUpdater;
        this.scopusFileConverter = scopusFileConverter;
    }

    @Override
    public ImportCandidate handleRequest(S3Event event, Context context) {
        return attempt(() -> createImportCandidate(event))
                   .map(this::updateExistingIfNeeded)
                   .flatMap(this::persistOrUpdateInDatabase)
                   .map(publication -> storeSuccessReport(publication, event))
                   .orElseThrow(fail -> handleSavingError(fail, event));
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

    private static ImportResult<String> generateReportFromContent(Failure<ImportCandidate> fail, String content) {
        return ImportResult.reportFailure(content, fail.getException());
    }

    private ImportCandidate updateExistingIfNeeded(ImportCandidate importCandidate) throws NotFoundException {
        return scopusUpdater.updateImportCandidate(importCandidate);
    }

    private ImportCandidate storeSuccessReport(ImportCandidate importCandidate, S3Event event) {
        return attempt(this::getS3DriverForScopusImportBucket).map(
            s3Driver -> insertSucceededReportFile(importCandidate, event, s3Driver)).orElseThrow();
    }

    private ImportCandidate insertSucceededReportFile(ImportCandidate importCandidate, S3Event event, S3Driver s3Driver)
        throws IOException {
        s3Driver.insertFile(constructFileUri(event, importCandidate).toS3bucketPath(),
                            getScopusIdentifier(importCandidate));
        return importCandidate;
    }

    private S3Driver getS3DriverForScopusImportBucket() {
        return new S3Driver(s3Client, new Environment().readEnv(SCOPUS_IMPORT_BUCKET));
    }

    private UriWrapper constructFileUri(S3Event s3Event, Publication publication) {
        var timestamp = timePath(s3Event);
        return UriWrapper.fromUri(SUCCESS_BUCKET_PATH)
                   .addChild(timestamp)
                   .addChild(publication.getIdentifier().toString());
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

    private RuntimeException handleSavingError(Failure<ImportCandidate> fail, S3Event event) {
        loggError(event, fail);
        saveReportToS3(fail, event);
        return ExceptionMapper.castToCorrectRuntimeException(fail.getException());
    }

    private void loggError(S3Event event, Failure<ImportCandidate> fail) {
        logger.error(ERROR_SAVING_SCOPUS_PUBLICATION, extractObjectKey(event), fail.getException());
    }

    private void saveReportToS3(Failure<ImportCandidate> fail, S3Event event) {
        attempt(() -> getContentToSave(event)).map(content -> generateReportFromContent(fail, content))
            .map(report -> insertReport(fail, event, report));
    }

    private URI insertReport(Failure<ImportCandidate> fail, S3Event event, ImportResult<String> report)
        throws IOException {
        return getS3DriverForScopusImportBucket().insertFile(
            constructErrorFileUri(event, fail.getException()).toS3bucketPath(), report.toJsonString());
    }

    private String getContentToSave(S3Event event) {
        return attempt(() -> readFile(event)).orElseThrow();
    }

    private UriWrapper constructErrorFileUri(S3Event event, Exception exception) {
        var fileUri = UriWrapper.fromUri(extractObjectKey(event));
        var timestamp = timePath(event);
        return UriWrapper.fromUri(ERROR_BUCKET_PATH
                                  + PATH_SEPERATOR
                                  + timestamp
                                  + PATH_SEPERATOR
                                  + exception.getClass().getSimpleName()
                                  + PATH_SEPERATOR
                                  + fileUri.getLastPathElement());
    }

    private String timePath(S3Event event) {
        return event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
    }

    private String extractObjectKey(S3Event s3Event) {
        return s3Event.getRecords().getFirst().getS3().getObject().getKey();
    }

    private Try<ImportCandidate> persistOrUpdateInDatabase(ImportCandidate importCandidate) throws BadRequestException {
        if (nonNull(importCandidate.getIdentifier())) {
            return Try.of(resourceService.updateImportCandidate(importCandidate));
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
        return resourceService.persistImportCandidate(importCandidate);
    }

    private ImportCandidate createImportCandidate(S3Event event) {
        return attempt(() -> readFile(event)).map(this::parseXmlFile)
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

    private String readFile(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath());
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().getFirst().getS3().getBucket().getName();
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().getFirst().getS3().getObject().getKey();
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractFilename(s3Event)));
    }
}
