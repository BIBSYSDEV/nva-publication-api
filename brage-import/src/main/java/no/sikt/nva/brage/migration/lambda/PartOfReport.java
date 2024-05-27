package no.sikt.nva.brage.migration.lambda;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import software.amazon.awssdk.services.s3.S3Client;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record PartOfReport(Publication publication, Record record) implements JsonSerializable {

    public static final String PART_OF = "PART_OF";
    private static final String BRAGE_MIGRATION_BUCKET = "BRAGE_MIGRATION_ERROR_BUCKET_NAME";

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PartOfReport that)) {
            return false;
        }
        return Objects.equals(record, that.record) && Objects.equals(publication, that.publication);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(publication, record);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return this.toJsonString();
    }

    public void persist(S3Client s3Client, String timeStamp) {
        var fileUri = createPartOfReportS3Location(timeStamp);
        var s3Driver = new S3Driver(s3Client, new Environment().readEnv(BRAGE_MIGRATION_BUCKET));
        attempt(() -> s3Driver.insertFile(fileUri.toS3bucketPath(), this.toJsonString())).orElseThrow();
    }

    private UriWrapper createPartOfReportS3Location(String timeStamp) {
        return UriWrapper.fromUri(PART_OF)
                   .addChild(record.getCustomer().getName())
                   .addChild(timeStamp)
                   .addChild(record.getId().getPath())
                   .addChild(publication.getIdentifier().toString());
    }
}
