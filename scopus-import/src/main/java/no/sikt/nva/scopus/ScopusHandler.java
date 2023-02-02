package no.sikt.nva.scopus;

import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXB;
import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.CristinConnection;
import no.sikt.nva.scopus.conversion.PiaConnection;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusHandler implements RequestHandler<S3Event, Publication> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    public static final String EVENT_TOPIC_IS_SET_IN_EVENT_BODY = "ReferToEventTopic";
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
    private static final String SCOPUS_EVENTS_FOLDER = "scopus/create";
    //TODO: move to config class.
    private static final String EVENTS_BUCKET = new Environment().readEnv("EVENTS_BUCKET");
    private static final String EVENT_BUS = new Environment().readEnv("EVENTS_BUS");
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;

    @JacocoGenerated
    public ScopusHandler() {
        this(S3Driver.defaultS3Client().build(), defaultEventBridgeClient(),
             defaultPiaConnection(), defaultCristinConnection());
    }

    public ScopusHandler(S3Client s3Client, EventBridgeClient eventBridgeClient,
                         PiaConnection piaConnection, CristinConnection cristinConnection) {
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
    }

    @Override
    public Publication handleRequest(S3Event event, Context context) {
        var publication = createPublication(event);
        emitEventToEventBridge(context, publication);
        return publication;
    }

    @JacocoGenerated
    private static EventBridgeClient defaultEventBridgeClient() {
        //TODO: setup the client properly
        return EventBridgeClient.create();
    }

    @JacocoGenerated
    private static PiaConnection defaultPiaConnection() {
        return new PiaConnection();
    }

    @JacocoGenerated
    private static CristinConnection defaultCristinConnection() {
        return new CristinConnection();
    }

    private void emitEventToEventBridge(Context context, Publication publication) {
        var fileLocation = writePublicationToS3(publication);
        var eventToEmit = new NewScopusEntryEvent(fileLocation);
        eventBridgeClient.putEvents(createPutEventRequest(eventToEmit, context));
    }

    private URI writePublicationToS3(Publication publication) {
        var s3Writer = new S3Driver(s3Client, EVENTS_BUCKET);
        return attempt(() -> constructPathForEventBody(publication))
                   .map(path -> s3Writer.insertFile(path, new ObjectMapper().writeValueAsString(publication)))
                   .orElseThrow();
    }

    private PutEventsRequest createPutEventRequest(EventReference eventToEmit, Context context) {
        var entry = PutEventsRequestEntry.builder()
                        .detailType(EVENT_TOPIC_IS_SET_IN_EVENT_BODY)
                        .time(Instant.now())
                        .eventBusName(EVENT_BUS)
                        .detail(eventToEmit.toJsonString())
                        .source(context.getFunctionName())
                        .resources(context.getInvokedFunctionArn())
                        .build();
        return PutEventsRequest.builder().entries(entry).build();
    }

    private Publication createPublication(S3Event event) {
        return attempt(() -> readFile(event)).map(this::parseXmlFile)
                   .map(this::generatePublication)
                   .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    private UnixPath constructPathForEventBody(Publication publication) {
        return UnixPath.of(SCOPUS_EVENTS_FOLDER, extractOneOfPossiblyManyScopusIdentifiers(publication));
    }

    private String extractOneOfPossiblyManyScopusIdentifiers(Publication publication) {
        return publication.getAdditionalIdentifiers()
                   .stream()
                   .filter(identifier -> ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME.equals(identifier.getSource()))
                   .map(AdditionalIdentifier::getValue)
                   .findFirst()
                   .orElseThrow();
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
