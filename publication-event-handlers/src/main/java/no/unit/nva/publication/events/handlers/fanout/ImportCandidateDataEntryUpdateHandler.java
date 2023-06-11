package no.unit.nva.publication.events.handlers.fanout;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toEntity;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ImportCandidateDataEntryUpdateHandler extends EventHandler<EventReference, EventReference> {

    public static final ImportCandidate NO_VALUE = null;
    public static final URI BLOB_IS_EMPTY = null;
    public static final EventReference DO_NOT_EMIT_EVENT = null;
    private static final Logger logger = LoggerFactory.getLogger(ImportCandidateDataEntryUpdateHandler.class);
    private final S3Driver s3Driver;

    @JacocoGenerated
    public ImportCandidateDataEntryUpdateHandler() {
        this(S3Driver.defaultS3Client().build());
    }

    public ImportCandidateDataEntryUpdateHandler(S3Client s3Client) {
        super(EventReference.class);
        this.s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
    }

    @Override
    protected EventReference processInput(EventReference input, AwsEventBridgeEvent<EventReference> event,
                                          Context context) {
        var s3Content = readBlobFromS3(input);
        var dynamoDbRecord = parseDynamoDbRecord(s3Content);
        var importCandidateDataEntryUpdate = convertToUpdateEvent(dynamoDbRecord);
        return attempt(() -> saveBlobToS3(importCandidateDataEntryUpdate))
                   .toOptional()
                   .map(blobUri -> new EventReference(importCandidateDataEntryUpdate.getTopic(), blobUri))
                   .map(this::logEvent)
                   .orElse(DO_NOT_EMIT_EVENT);
    }

    private EventReference logEvent(EventReference event) {
        logger.debug("Emitted Event:{}", event.toJsonString());
        return event;
    }

    private URI saveBlobToS3(ImportCandidateDataEntryUpdate blob) throws IOException {
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        return blob.notEmpty()
                   ? s3Driver.insertFile(filePath, blob.toJsonString())
                   : BLOB_IS_EMPTY;
    }

    private ImportCandidateDataEntryUpdate convertToUpdateEvent(DynamodbStreamRecord dynamoDbRecord) {
        return new ImportCandidateDataEntryUpdate(
            dynamoDbRecord.getEventName(),
            getImportCandidate(dynamoDbRecord.getDynamodb().getOldImage()),
            getImportCandidate(dynamoDbRecord.getDynamodb().getNewImage())
        );
    }

    private ImportCandidate getImportCandidate(Map<String, AttributeValue> image) {
        return
            attempt(() -> toEntity(image)).toOptional(this::logFailureInDebugging)
                .flatMap(Function.identity())
                .map(this::castToImportCandidate).orElse(NO_VALUE);
    }

    private ImportCandidate castToImportCandidate(Entity entity) {
        return ((Resource) entity).toImportCandidate();
    }

    private void logFailureInDebugging(Failure<Optional<Entity>> fail) {
        logger.debug(ExceptionUtils.stackTraceInSingleLine(fail.getException()));
    }

    private String readBlobFromS3(EventReference input) {
        logger.info("Event to proceed: {}", input.getUri());
        var filePath = UriWrapper.fromUri(input.getUri()).toS3bucketPath();
        return s3Driver.getFile(filePath);
    }

    private DynamodbStreamRecord parseDynamoDbRecord(String s3Content) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(s3Content, DynamodbStreamRecord.class)).orElseThrow();
    }
}
