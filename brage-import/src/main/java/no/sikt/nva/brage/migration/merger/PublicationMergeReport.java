package no.sikt.nva.brage.migration.merger;

import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.BRAGE_MIGRATION_REPORTS_BUCKET_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.s3.S3Client;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record PublicationMergeReport(Map<String, MergeResult> mergeReport, SortableIdentifier publicationIdentifier)
    implements JsonSerializable {

    public static PublicationMergeReport fromString(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, PublicationMergeReport.class)).orElseThrow();
    }

    public static Optional<PublicationMergeReport> fetch(String publicationIdentifier, S3Client s3Client) {
        try {
            var fileUri = publicationUpdateReportPath(publicationIdentifier);
            var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
            return Optional.of(s3Driver.getFile(fileUri)).map(PublicationMergeReport::fromString);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public static PublicationMergeReport createEmptyReport(SortableIdentifier publicationIdentifier) {
        return new PublicationMergeReport(new HashMap<>(), publicationIdentifier);
    }

    public PublicationMergeReport persist(S3Client s3Client) {
        var fileUri = publicationUpdateReportPath(publicationIdentifier.toString());
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_REPORTS_BUCKET_NAME));
        attempt(() -> s3Driver.insertFile(fileUri, this.toString())).orElseThrow();
        return this;
    }

    public void addNewMergeResult(String institution, Publication institutionImage, Publication oldImage,
                                  Publication newImage) {
        mergeReport.put(institution, new MergeResult(institutionImage, oldImage, newImage));
    }

    @Override
    public String toString() {
        return this.toJsonString();
    }

    private static UnixPath publicationUpdateReportPath(String publicationIdentifier) {
        return UriWrapper.fromUri("PUBLICATION_UPDATE").addChild(publicationIdentifier).toS3bucketPath();
    }


}
