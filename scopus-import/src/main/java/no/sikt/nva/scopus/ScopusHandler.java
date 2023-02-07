package no.sikt.nva.scopus;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;
import java.io.StringReader;
import java.net.URI;
import java.util.Random;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.CristinConnection;
import no.sikt.nva.scopus.conversion.PiaConnection;
import no.sikt.nva.scopus.exception.ExceptionMapper;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusHandler implements RequestHandler<S3Event, Publication> {

    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final int MAX_EFFORTS = 10;
    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final int MAX_SLEEP_TIME = 100;
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
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
                   .orElseThrow(this::handleSavingError);
    }

    @JacocoGenerated
    private static PiaConnection defaultPiaConnection() {
        return new PiaConnection();
    }

    @JacocoGenerated
    private static CristinConnection defaultCristinConnection() {
        return new CristinConnection();
    }

    private RuntimeException handleSavingError(Failure<Publication> fail) {
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
