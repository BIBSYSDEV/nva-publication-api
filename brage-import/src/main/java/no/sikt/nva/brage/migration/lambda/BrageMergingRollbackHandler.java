package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATED_PUBLICATIONS_REPORTS_PATH;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class BrageMergingRollbackHandler extends EventHandler<EventReference, Void> {
    private static final Logger logger = LoggerFactory.getLogger(BrageMergingRollbackHandler.class);

    public final static String TOPIC = "BrageMerging.Rollback.Request";
    public final static String ERROR_MERGING_ROLLBACK = "ERROR_MERGING_ROLLBACK";

    private final S3Client s3Client;

    private String previousVersion;

    @JacocoGenerated
    public BrageMergingRollbackHandler() {
        this(S3Driver.defaultS3Client().build());
    }

    public BrageMergingRollbackHandler(S3Client s3Client) {
        super(EventReference.class);
        this.s3Client = s3Client;
    }

    @Override
    protected Void processInput(EventReference eventReference,
                                AwsEventBridgeEvent<EventReference> event,
                                Context context) {
        resetLambda();
        attempt(() -> readOldPublicationFromS3(eventReference))
            .orElse(fail -> handleSavingError(fail, eventReference));
        return null;
    }

    private void resetLambda() {
        previousVersion = null;
    }

    private Publication handleSavingError(Failure<Publication> fail, EventReference eventReference) {
        String errorMessage = ERROR_MERGING_ROLLBACK + extractNvaIdentifier(eventReference.getUri());
        logger.error(errorMessage, fail.getException());
        saveReportToS3(fail, eventReference);
        return null;
    }

    private String extractNvaIdentifier(URI uri) {
        return UriWrapper.fromUri(uri).getLastPathElement();
    }

    private void saveReportToS3(Failure<Publication> fail,
                                EventReference event) {
        var errorFileUri = constructErrorFileUri(event, fail.getException());
        var s3Driver = new S3Driver(s3Client, event.extractBucketName());
        var content = determineBestEventReference(event);
        var reportContent = ImportResult.reportFailure(content, fail.getException());
        attempt(() -> s3Driver.insertFile(errorFileUri.toS3bucketPath(), reportContent.toJsonString())).orElseThrow();
    }

    private UriWrapper constructErrorFileUri(EventReference event,
                                             Exception exception) {
        var uriString = event.getUri().toString().replace(UPDATED_PUBLICATIONS_REPORTS_PATH,
                                                          ERROR_MERGING_ROLLBACK + "/" + exception.getClass()
                                                                                             .getSimpleName());
        return UriWrapper.fromUri(uriString);
    }

    private String determineBestEventReference(EventReference event) {
        if (StringUtils.isNotBlank(previousVersion)) {
            return previousVersion;
        } else {
            return extractNvaIdentifier(event.getUri());
        }
    }

    private Publication readOldPublicationFromS3(EventReference eventReference) throws JsonProcessingException {
        previousVersion = readFromS3(eventReference);
        return JsonUtils.dtoObjectMapper.readValue(previousVersion, Publication.class);
    }

    private String readFromS3(EventReference eventReference) {
        var s3Driver = new S3Driver(s3Client, eventReference.extractBucketName());
        return s3Driver.readEvent(eventReference.getUri());
    }
}
