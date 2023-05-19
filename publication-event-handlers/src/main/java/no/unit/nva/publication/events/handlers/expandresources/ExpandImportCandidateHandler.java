package no.unit.nva.publication.events.handlers.expandresources;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.IOException;
import java.net.URI;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import software.amazon.awssdk.services.s3.S3Client;

public class ExpandImportCandidateHandler extends
                                            DestinationsEventBridgeEventHandler<EventReference, EventReference> {

    public static final String EVENTS_BUCKET = "EVENTS_BUCKET";
    public static final String HANDLER_EVENTS_FOLDER = "PublicationService-DataEntryExpansion";
    public static final String EVENT_TOPIC = "ImportCandidates.ExpandedDataEntry.Update";
    public static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    private final S3Driver s3Driver;

    @JacocoGenerated
    protected ExpandImportCandidateHandler() {
        this(new S3Driver(EVENTS_BUCKET));
    }

    public ExpandImportCandidateHandler(S3Client s3Client) {
        this(new S3Driver(s3Client, new Environment().readEnv(EVENTS_BUCKET)));
    }

    private ExpandImportCandidateHandler(S3Driver s3Driver) {
        super(EventReference.class);
        this.s3Driver = s3Driver;
    }

    @Override
    protected EventReference processInputPayload(EventReference input,
                                                 AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                                 Context context) {
        var blob = readBlobFromS3(input);
        var importCandidate = (ImportCandidate) blob.getNewData();
        return attempt(() -> ExpandedImportCandidate.fromImportCandidate(importCandidate))
                   .map(this::proceedEvent)
                   .map(this::toEventReference)
                   .orElse(failure -> emptyEvent());
    }

    private EventReference emptyEvent() {
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }

    private EventReference toEventReference(URI uri) {
        return new EventReference(EVENT_TOPIC, uri);
    }

    private URI proceedEvent(ExpandedImportCandidate expandedImportCandidate) throws IOException {
        return s3Driver.insertEvent(UnixPath.of(HANDLER_EVENTS_FOLDER), expandedImportCandidate.toJsonString());
    }

    private DataEntryUpdateEvent readBlobFromS3(EventReference input) {
        var blobString = s3Driver.readEvent(input.getUri());
        return DataEntryUpdateEvent.fromJson(blobString);
    }
}
