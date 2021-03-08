package no.unit.nva.publication.migration;

import static no.unit.nva.publication.migration.FakeS3Driver.duplicateWithLaterModifiedDate;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.s3.S3Driver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class PublicationImporterTest extends AbstractDataMigrationTest {

    private static final String EXISTING_REMOTE_BUCKET_NAME = "orestis-export";
    private static final Path EXISTING_DATA_PATH = Path.of("AWSDynamoDB",
        "01614701350715-b7f02d9e", "data/");

    private PublicationImporter publicationImporter;
    private ResourceService resourceService;


    @BeforeEach
    public void init() {
        super.init();
        AmazonDynamoDB dynamoClient = super.client;
        S3Driver mockS3client = new FakeS3Driver();
        resourceService = new ResourceService(dynamoClient, Clock.systemDefaultZone());
        publicationImporter = new PublicationImporter(mockS3client, EXISTING_DATA_PATH, resourceService);
    }

    @Test
    @Tag("RemoteTest")
    public void publicationImporterReturnsListOfPublicationsWhenRemotePathContainsPublications() {
        publicationImporter = new PublicationImporter(remoteS3Connection(), EXISTING_DATA_PATH, resourceService);
        List<Publication> publications = publicationImporter.getPublications();
        assertThat(publications, is(not(empty())));
    }

    @Test
    @Tag("RemoteTest")
    public void getPublicationsReturnsOnePublicationInstancePerIdentifier() {
        publicationImporter = new PublicationImporter(remoteS3Connection(), EXISTING_DATA_PATH, resourceService);
        List<Publication> publications = publicationImporter.getPublications();
        Set<SortableIdentifier> identifiers = publications.stream()
                                                  .map(Publication::getIdentifier)
                                                  .collect(Collectors.toSet());
        assertThat(publications.size(), is(equalTo(identifiers.size())));
    }

    @Test
    @Tag("RemoteTest")
    public void createResourcesReturnsOneResourcePerPublicationInRemoteFolder() {
        publicationImporter = new PublicationImporter(remoteS3Connection(), EXISTING_DATA_PATH, resourceService);
        List<Publication> publications = publicationImporter.getPublications();
        List<Resource> resources = publicationImporter.createResources(publications);
        assertThat(resources.size(), is(equalTo(publications.size())));
    }

    @Test
    @Tag("RemoteTest")
    public void insertPublicationsInsertsRemotePublicationsToRemoteDynamoDb() throws IOException {
        publicationImporter = new PublicationImporter(remoteS3Connection(), EXISTING_DATA_PATH, resourceService);
        List<Publication> publications = publicationImporter.getPublications();
        List<ResourceUpdate> result = publicationImporter.insertPublications(publications);

        var reportGenerator = new ReportGenerator(result);
        reportGenerator.writeFailures();
        reportGenerator.writeDifferences();
    }

    @Test
    public void createResourcesReturnsOneResourcePerPublication() {
        List<Publication> publications = publicationImporter.getPublications();
        List<Resource> resources = publicationImporter.createResources(publications);
        assertThat(resources.size(), is(equalTo(EXPECTED_IMPORTED_PUBLICATIONS.size())));
    }

    @Test
    public void publicationImporterReturnsListOfLatestVersionsOfPublicationsWhenPathContainsPublications() {
        List<Publication> publications = publicationImporter.getPublications();

        Publication[] expectedPublications = EXPECTED_IMPORTED_PUBLICATIONS.toArray(new Publication[0]);
        assertThat(publications, containsInAnyOrder(expectedPublications));
    }

    @Test
    public void removeDuplicatesReturnsSinglePublicationIfOnePublicationExistsForOneIdentifier() {
        var firstPublication = PublicationGenerator.publicationWithIdentifier();
        var secondPublication = PublicationGenerator.publicationWithIdentifier();
        var laterPublication = duplicateWithLaterModifiedDate(secondPublication);
        var publicationStream = Stream.of(firstPublication, secondPublication, laterPublication);

        var actualPublicationList = PublicationImporter.removeDuplicates(publicationStream);
        assertThat(actualPublicationList, containsInAnyOrder(firstPublication, laterPublication));
    }

    @Test
    public void removeDuplicatesReturnsLatestPublicationIfMoreThanOnePublicationsExistForOneIdentifier() {
        var earlyPublication = PublicationGenerator.publicationWithIdentifier();
        var duplicate = duplicateWithLaterModifiedDate(earlyPublication);
        var publicationStream = Stream.of(earlyPublication, duplicate);
        var actualPublicationList = PublicationImporter.removeDuplicates(publicationStream);
        assertThat(actualPublicationList, contains(duplicate));
    }

    private S3Driver remoteS3Connection() {
        return attempt(() -> new S3Driver(S3Client.create(), EXISTING_REMOTE_BUCKET_NAME))
                   .orElse(fail -> null);
    }
}