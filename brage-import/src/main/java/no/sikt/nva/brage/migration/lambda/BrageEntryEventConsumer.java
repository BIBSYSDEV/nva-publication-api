package no.sikt.nva.brage.migration.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import no.sikt.nva.brage.migration.AssociatedArtifactMover;
import no.sikt.nva.brage.migration.mapper.BrageNvaMapper;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class BrageEntryEventConsumer implements RequestHandler<S3Event, Publication> {

    private static final int SINGLE_EXPECTED_RECORD = 0;
    private static final String S3_URI_TEMPLATE = "s3://%s/%s";

    private static final Logger logger = LoggerFactory.getLogger(BrageEntryEventConsumer.class);
    private final S3Client s3Client;

    public BrageEntryEventConsumer(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @JacocoGenerated
    public BrageEntryEventConsumer() {
        this(S3Driver.defaultS3Client().build());
    }

    @Override
    public Publication handleRequest(S3Event s3Event, Context context) {
        return attempt(() -> parseBrageRecord(s3Event))
                   .map(publication -> pushAssociatedFilesToPersistedStorage(publication, s3Event))
                   .orElseThrow(this::handleSavingError);
    }

    private Publication pushAssociatedFilesToPersistedStorage(Publication publication, S3Event s3Event) {
        var associatedArtifactMover = new AssociatedArtifactMover(s3Client, s3Event);
        associatedArtifactMover.pushAssociatedArtifactsToPersistedStorage(publication);
        return publication;
    }

    private RuntimeException handleSavingError(Failure<Publication> fail) {
        logger.error("Could not convert brage record to nva publication");
        return new RuntimeException(fail.getException());
    }

    private Publication parseBrageRecord(S3Event event)
        throws JsonProcessingException, InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var record = getBrageRecordFromS3(event);
        return convertBrageRecordToNvaPublication(record);
    }

    private Publication convertBrageRecordToNvaPublication(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return BrageNvaMapper.toNvaPublication(record);
    }

    private Record getBrageRecordFromS3(S3Event event) throws JsonProcessingException {
        var brageRecordFile = readFileFromS3(event);
        return parseBrageRecordJson(brageRecordFile);
    }

    private Record parseBrageRecordJson(String brageRecordFile) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(brageRecordFile, Record.class);
    }

    private String readFileFromS3(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath());
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractFilename(s3Event)));
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getBucket().getName();
    }
}
