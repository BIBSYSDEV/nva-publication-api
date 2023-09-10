package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.publication.events.bodies.DeleteImportCandidateEvent.EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.s3.S3Driver.defaultS3Client;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.events.bodies.DeleteImportCandidateEvent;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteImportCandidateEventHandler
    extends DestinationsEventBridgeEventHandler<EventReference, DeleteImportCandidateEvent> {

    private final S3Client s3Client;

    @JacocoGenerated
    public DeleteImportCandidateEventHandler() {
        this(defaultS3Client().build());
    }

    protected DeleteImportCandidateEventHandler(S3Client s3Client) {
        super(EventReference.class);
        this.s3Client = s3Client;
    }

    @Override
    protected DeleteImportCandidateEvent processInputPayload(
        EventReference input,
        AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
        Context context) {
        var blob = readBlobFromS3(input);
        return new DeleteImportCandidateEvent(EVENT_TOPIC, blob.getOldData().getIdentifier());
    }

    private ImportCandidateDataEntryUpdate readBlobFromS3(EventReference input) {
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        var blobString = s3Driver.readEvent(input.getUri());
        return ImportCandidateDataEntryUpdate.fromJson(blobString);
    }
}
