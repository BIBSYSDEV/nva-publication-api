package no.sikt.nva.brage.migration.lambda;

import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.UPDATED_PUBLICATIONS_REPORTS_PATH;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import no.sikt.nva.brage.migration.merger.BrageMergingReport;
import no.sikt.nva.brage.migration.rollback.RollBackConflictException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.NotFoundException;
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

    public final static String NEWER_VERSION_OF_THE_PUBLICATION_EXISTS = "Newer version of the publication exists";

    private static final String INIT_VALUE = null;
    private final S3Client s3Client;
    private final ResourceService resourceService;

    private String previousVersion;

    @JacocoGenerated
    public BrageMergingRollbackHandler() {
        this(S3Driver.defaultS3Client().build(), ResourceService.defaultService());
    }

    public BrageMergingRollbackHandler(S3Client s3Client, ResourceService resourceService) {
        super(EventReference.class);
        this.s3Client = s3Client;
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInput(EventReference eventReference,
                                AwsEventBridgeEvent<EventReference> event,
                                Context context) {
        resetLambda();
        attempt(() -> readMergeReportFromS3(eventReference))
            .map(this::processReport)
            .orElse(fail -> handleSavingError(fail, eventReference));
        return null;
    }

    private BrageMergingReport processReport(BrageMergingReport mergeReport) throws NotFoundException {
        validateCurrentVersion(mergeReport);
        return mergeReport;
    }

    private void validateCurrentVersion(BrageMergingReport mergeReport) throws NotFoundException {
        var currentVersionOfPublication = resourceService.getPublication(mergeReport.newImage());
        if (publicationHasBeenModifiedAfterBrageMerging(mergeReport, currentVersionOfPublication)) {
            throw new RollBackConflictException(NEWER_VERSION_OF_THE_PUBLICATION_EXISTS);
        }
    }

    private static boolean publicationHasBeenModifiedAfterBrageMerging(BrageMergingReport mergeReport, Publication currentVersionOfPublication) {
        return !currentVersionOfPublication.equals(mergeReport.newImage());
    }

    private void resetLambda() {
        previousVersion = INIT_VALUE;
    }

    private BrageMergingReport handleSavingError(Failure<BrageMergingReport> fail, EventReference eventReference) {
        String errorMessage = ERROR_MERGING_ROLLBACK + extractNvaIdentifier(eventReference.getUri());
        logger.error(errorMessage, fail.getException());
        saveReportToS3(fail, eventReference);
        return null;
    }

    private String extractNvaIdentifier(URI uri) {
        return UriWrapper.fromUri(uri).getLastPathElement();
    }

    private void saveReportToS3(Failure<BrageMergingReport> fail,
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

    private BrageMergingReport readMergeReportFromS3(EventReference eventReference) throws JsonProcessingException {
        previousVersion = readFromS3(eventReference);
        return JsonUtils.dtoObjectMapper.readValue(previousVersion, BrageMergingReport.class);
    }

    private String readFromS3(EventReference eventReference) {
        var s3Driver = new S3Driver(s3Client, eventReference.extractBucketName());
        return s3Driver.readEvent(eventReference.getUri());
    }
}
