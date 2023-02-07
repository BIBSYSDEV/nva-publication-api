package no.sikt.nva.scopus;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Random;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.CristinConnection;
import no.sikt.nva.scopus.conversion.PiaConnection;
import no.sikt.nva.scopus.exception.ExceptionMapper;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusHandler implements RequestHandler<S3Event, Publication> {

    public static final String YYYY_MM_DD_HH_FORMAT = "yyyy-MM-dd:HH";
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final int MAX_EFFORTS = 10;
    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    public static final String PATH_SEPERATOR = "/";
    public static final String SCOPUS_IMPORT_BUCKET = "SCOPUS_IMPORT_BUCKET";
    public static final String SCOPUS_IDENTIFIER = "scopusIdentifier";
    private static final String ERROR_SAVING_SCOPUS_PUBLICATION = "Error saving imported scopus publication object "
                                                                  + "key: ";
    private static final int MAX_SLEEP_TIME = 100;
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
    private static final String ERROR_BUCKET_PATH = "ERROR";
    public static final String SUCCESS_BUCKET_PATH = "SUCCESS";
    private final S3Client s3Client;
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;
    private final ResourceService resourceService;

    @JacocoGenerated
    public ScopusHandler() {
        this(S3Driver.defaultS3Client().build(), defaultPiaConnection(), defaultCristinConnection(),
             ResourceService.defaultService());
    }

    public ScopusHandler(S3Client s3Client, PiaConnection piaConnection, CristinConnection cristinConnection,
                         ResourceService resourceService) {
        this.s3Client = s3Client;
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
        this.resourceService = resourceService;
    }

    @Override
    public Publication handleRequest(S3Event event, Context context) {
        return attempt(() -> createPublication(event))
                   .flatMap(this::persistInDatabase)
                   .map(publication -> storeSuccessReport(publication, event))
                   .orElseThrow(fail -> handleSavingError(fail, event));
    }

    @JacocoGenerated
    private static PiaConnection defaultPiaConnection() {
        return new PiaConnection();
    }

    @JacocoGenerated
    private static CristinConnection defaultCristinConnection() {
        return new CristinConnection();
    }

    private static ImportResult<String> generateReportFromContent(Failure<Publication> fail, String content) {
        return ImportResult.reportFailure(content, fail.getException());
    }

    private Publication storeSuccessReport(Publication publication, S3Event event) {
        return attempt(this::getS3DriverForScopusImportBucket)
                   .map(s3Driver -> insertSucceededReportFile(publication, event, s3Driver))
                   .orElseThrow();
    }

    private Publication insertSucceededReportFile(Publication publication, S3Event event, S3Driver s3Driver)
        throws IOException {
        s3Driver.insertFile(constructFileUri(event, publication).toS3bucketPath(),
                            getScopusIdentifier(publication));
        return publication;
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

    private String getScopusIdentifier(Publication publication) {
        return publication.getAdditionalIdentifiers()
                   .stream()
                   .filter(this::isScopusIdentifier)
                   .map(AdditionalIdentifier::getValue)
                   .findFirst()
                   .orElse(null);
    }

    private boolean isScopusIdentifier(AdditionalIdentifier identifier) {
        return SCOPUS_IDENTIFIER.equals(identifier.getSource());
    }

    private RuntimeException handleSavingError(Failure<Publication> fail, S3Event event) {
        loggError(event, fail);
        saveReportToS3(fail, event);
        return ExceptionMapper.castToCorrectRuntimeException(fail.getException());
    }

    private void loggError(S3Event event, Failure<Publication> fail) {
        logger.error(ERROR_SAVING_SCOPUS_PUBLICATION + extractObjectKey(event), fail.getException());
    }

    private void saveReportToS3(Failure<Publication> fail, S3Event event) {
        attempt(() -> getContentToSave(event))
            .map(content -> generateReportFromContent(fail, content))
            .map(report -> insertReport(fail, event, report));
    }

    private URI insertReport(Failure<Publication> fail, S3Event event,
                             ImportResult<String> report)
        throws IOException {
        return getS3DriverForScopusImportBucket()
                   .insertFile(constructErrorFileUri(event, fail.getException()).toS3bucketPath(),
                               report.toJsonString());
    }

    private String getContentToSave(S3Event event) {
        return attempt(() -> readFile(event)).orElseThrow();
    }

    private UriWrapper constructErrorFileUri(S3Event event, Exception exception) {
        var fileUri = UriWrapper.fromUri(extractObjectKey(event));
        var timestamp = timePath(event);
        return UriWrapper.fromUri(ERROR_BUCKET_PATH + PATH_SEPERATOR
                                  + timestamp + PATH_SEPERATOR + exception.getClass().getSimpleName() +
                                  PATH_SEPERATOR + fileUri.getLastPathElement());
    }

    private String timePath(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getEventTime().toString(YYYY_MM_DD_HH_FORMAT);
    }

    private String extractObjectKey(S3Event s3Event) {
        return s3Event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
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

    private Publication createPublication(S3Event event) {
        return attempt(() -> readFile(event)).map(this::parseXmlFile)
                   .map(this::generatePublication)
                   .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    private RuntimeException logErrorAndThrowException(Exception exception) {
        logger.error(exception.getMessage());
        return exception instanceof RuntimeException ? (RuntimeException) exception : new RuntimeException(exception);
    }

    private DocTp parseXmlFile(String file) {
        return JAXB.unmarshal(new StringReader(file), DocTp.class);
    }

    private Publication generatePublication(DocTp docTp) {
        var scopusConverter = new ScopusConverter(docTp, piaConnection, cristinConnection);
        return scopusConverter.generatePublication();
    }

    private String readFile(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath());
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getBucket().getName();
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractFilename(s3Event)));
    }
}
