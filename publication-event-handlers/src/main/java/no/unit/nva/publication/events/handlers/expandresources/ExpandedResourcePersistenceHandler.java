package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.ENVIRONMENT;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static no.unit.nva.publication.events.handlers.expandresources.IndexDocument.createIndexDocument;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.expansion.model.ExpandedDatabaseEntry;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;

public class ExpandedResourcePersistenceHandler
    extends DestinationsEventBridgeEventHandler<EventPayload, EventPayload> {

    private final S3Driver s3Reader;
    private final S3Driver s3Writer;
    public static final String PERSISTED_ENTRIES_BUCKET = ENVIRONMENT.readEnv("PERSISTED_ENTRIES_BUCKET");

    @JacocoGenerated
    public ExpandedResourcePersistenceHandler() {
        this(new S3Driver(PublicationEventsConfig.EVENTS_BUCKET), new S3Driver(PERSISTED_ENTRIES_BUCKET));
    }

    protected ExpandedResourcePersistenceHandler(S3Driver s3Reader, S3Driver s3Writer) {
        super(EventPayload.class);
        this.s3Reader = s3Reader;
        this.s3Writer = s3Writer;
    }

    @Override
    protected EventPayload processInputPayload(EventPayload input,
                                               AwsEventBridgeEvent<AwsEventBridgeDetail<EventPayload>> event,
                                               Context context) {
        String data = s3Reader.readEvent(input.getPayloadUri());
        var expandedResourceUpdate =
            attempt(() -> dynamoImageSerializerRemovingEmptyFields.readValue(data, ExpandedDatabaseEntry.class))
                .orElseThrow();
        var indexDocument = createIndexDocument(expandedResourceUpdate);
        URI uri = attempt(() -> s3Writer.insertEvent(UnixPath.EMPTY_PATH, indexDocument.toJsonString())).orElseThrow();
        return EventPayload.indexEntryEvent(uri);
    }
}
