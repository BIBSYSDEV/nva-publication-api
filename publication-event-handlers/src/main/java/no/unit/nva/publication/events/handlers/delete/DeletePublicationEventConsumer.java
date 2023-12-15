package no.unit.nva.publication.events.handlers.delete;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.persistence.PersistenceConfig.PERSISTED_ENTRIES_BUCKET;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.events.bodies.DeleteResourceEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

public class DeletePublicationEventConsumer extends
                                            DestinationsEventBridgeEventHandler<EventReference,
                                                                                   DeleteResourceEvent> {

    public static final String RESOURCES = "resources/";
    public static final String RECEIVED_EVENTREFERENCE = "Received eventreference: ";
    public static final String DELETING_PUBLICATION = "Deleting publication: ";
    private static final Logger logger = LoggerFactory.getLogger(DeletePublicationEventConsumer.class);
    private final S3Client s3Client;

    @JacocoGenerated
    public DeletePublicationEventConsumer() {
        this(defaultS3Client());
    }

    public DeletePublicationEventConsumer(S3Client s3Client) {
        super(EventReference.class);
        this.s3Client = s3Client;
    }

    @Override
    protected DeleteResourceEvent processInputPayload(EventReference input,
                                                      AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                      Context context) {
        logger.info(RECEIVED_EVENTREFERENCE + input.toJsonString());
        var publication = readEvent(input);
        if (isDeleted(publication)) {
            logger.info(DELETING_PUBLICATION + publication.getIdentifier().toString());
            deletePublicationFromPersistedBucket(publication);
            return toDeletePublicationEvent(publication);
        }
        return null;
    }

    private static DeleteObjectRequest createDeleteObjectRequest(Publication publication) {
        return DeleteObjectRequest.builder()
                   .bucket(PERSISTED_ENTRIES_BUCKET)
                   .key(RESOURCES + publication.getIdentifier().toString())
                   .build();
    }

    @JacocoGenerated
    private static S3Client defaultS3Client() {
        return S3Client.builder()
                   .httpClient(UrlConnectionHttpClient.create())
                   .build();
    }

    private static boolean hasSoftOrHardDeletedStatus(Publication publication) {
        return PublicationStatus.DELETED.equals(publication.getStatus())
               || PublicationStatus.UNPUBLISHED.equals(publication.getStatus());
    }

    private Publication readEvent(EventReference input) {
        var s3Reader = new S3Driver(s3Client, EVENTS_BUCKET);
        String data = s3Reader.readEvent(input.getUri());
        return attempt(() -> PublicationEventsConfig.objectMapper.readValue(data, Publication.class))
                   .orElseThrow();
    }

    private void deletePublicationFromPersistedBucket(Publication publication) {
        attempt(() -> s3Client.deleteObject(createDeleteObjectRequest(publication))).orElseThrow();
    }

    private DeleteResourceEvent toDeletePublicationEvent(Publication publication) {
        return new DeleteResourceEvent(DeleteResourceEvent.EVENT_TOPIC,
                                       publication.getIdentifier());
    }

    private boolean isDeleted(Publication publication) {
        return nonNull(publication)
               && hasSoftOrHardDeletedStatus(publication)
               && nonNull(publication.getIdentifier());
    }
}
