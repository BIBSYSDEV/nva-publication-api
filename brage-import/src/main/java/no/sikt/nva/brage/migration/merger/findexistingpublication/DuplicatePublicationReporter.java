package no.sikt.nva.brage.migration.merger.findexistingpublication;

import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.s3.S3Client;

public class DuplicatePublicationReporter {

    public static final String DUPLICATE_WARNING_PATH = "DUPLICATES_DETECTED";

    private final S3Client s3Client;
    private final String bucketName;

    public DuplicatePublicationReporter(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void reportDuplicatePublications(List<Publication> publications,
                                            Record brageRecord,
                                            DuplicateDetectionCause cause) {
        var duplicateReport = new DuplicateReport(getPublicationsIdentifiers(publications),
                                                  cause,
                                                  brageRecord.getId());
        var fileUri = constructFileUri(brageRecord, cause);
        var s3Driver = new S3Driver(s3Client, bucketName);
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), duplicateReport.toJsonString())).orElseThrow();
    }

    private UriWrapper constructFileUri(Record brageRecord, DuplicateDetectionCause cause) {
        return UriWrapper.fromUri(DUPLICATE_WARNING_PATH)
                   .addChild(brageRecord.getResourceOwner().getOwner().split("@")[0])
                   .addChild(cause.getValue())
                   .addChild(UriWrapper.fromUri(brageRecord.getId()).getLastPathElement());
    }

    private List<SortableIdentifier> getPublicationsIdentifiers(List<Publication> publications) {
        return publications.stream().map(Publication::getIdentifier).toList();
    }
}
