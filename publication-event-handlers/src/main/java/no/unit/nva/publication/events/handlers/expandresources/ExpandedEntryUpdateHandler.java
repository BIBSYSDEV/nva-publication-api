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
import no.unit.nva.expansion.model.ExpandedResourceUpdate;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;

public class ExpandedEntryUpdateHandler extends
                                        DestinationsEventBridgeEventHandler<EventPayload, EventPayload> {

    private final S3Driver s3Reader;
    private final S3Driver s3Writer;
    public static final String INDEX_ENTRIES_BUCKET = ENVIRONMENT.readEnv("INDEX_ENTRIES_BUCKET");

    @JacocoGenerated
    public ExpandedEntryUpdateHandler() {
        this(new S3Driver(PublicationEventsConfig.EVENTS_BUCKET), new S3Driver(INDEX_ENTRIES_BUCKET));
    }

    protected ExpandedEntryUpdateHandler(S3Driver s3Reader, S3Driver s3Writer) {
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
            attempt(() -> dynamoImageSerializerRemovingEmptyFields.readValue(data, ExpandedResourceUpdate.class))
                .orElseThrow();
        var indexDocument = createIndexDocument(expandedResourceUpdate);
        URI uri = attempt(() -> s3Writer.insertEvent(UnixPath.EMPTY_PATH, indexDocument.toJsonString())).orElseThrow();
        return EventPayload.indexEntryEvent(uri);
    }
}
