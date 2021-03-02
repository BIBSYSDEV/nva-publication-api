package no.unit.nva.publication.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import java.nio.file.Path;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.s3.S3Driver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class PublicationImporterTest {

    private static final String EXISTING_REMOTE_BUCKET_NAME = "orestis-export";
    private static final Path EXISTING_DATA_PATH = Path.of("AWSDynamoDB",
        "01614600960660-a9739099", "data/");
    PublicationImporter publicationImporter;

    @BeforeEach
    public void init() {
        S3Driver client = new S3Driver(S3Client.create(), EXISTING_REMOTE_BUCKET_NAME);
        publicationImporter = new PublicationImporter(client, EXISTING_DATA_PATH);
    }

    @Test
    @Tag("RemoteTest")
    public void publicationImporterReturnsListOfPublicationsWhenPathContainsPublications() {
        List<Publication> publications = publicationImporter.getPublications();
        assertThat(publications, is(not(empty())));
    }
}