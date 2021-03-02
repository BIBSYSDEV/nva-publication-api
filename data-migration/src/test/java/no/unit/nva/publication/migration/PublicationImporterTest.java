package no.unit.nva.publication.migration;

import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

public class PublicationImporterTest {

    private static final String EXISTING_REMOTE_BUCKET_NAME = "orestis-export";
    private static final Path EXISTING_DATA_PATH = Path.of("AWSDynamoDB",
        "01614600960660-a9739099", "data/");
    private static final List<Publication> FIRST_PUBLICATIONS_FILE = samplePublications();
    private static final List<Publication> SECOND_PUBLICATIONS_FILE = samplePublications();
    private static final List<Publication> EXPECTED_PUBLICATIONS = constructExpectedPublications();
    private PublicationImporter publicationImporter;

    @BeforeEach
    public void init() {
        S3Driver remoteClient = remoteConnection();
        S3Driver mockClient = new FakeS3Driver();
        publicationImporter = new PublicationImporter(mockClient, EXISTING_DATA_PATH);
    }

    @Test
    @Tag("RemoteTest")
    public void publicationImporterReturnsListOfPublicationsWhenRemotePathContainsPublications() {
        publicationImporter = new PublicationImporter(remoteConnection(), EXISTING_DATA_PATH);
        List<Publication> publications = publicationImporter.getPublications();
        assertThat(publications, is(not(empty())));
    }

    @Test
    @Tag("RemoteTest")
    public void getPublicationsReturnsOnePublicationInstancePerIdentifier() {
        publicationImporter = new PublicationImporter(remoteConnection(), EXISTING_DATA_PATH);
        List<Publication> publications = publicationImporter.getPublications();
        Set<SortableIdentifier> identifiers = publications.stream()
                                                  .map(Publication::getIdentifier)
                                                  .collect(Collectors.toSet());
        assertThat(publications.size(), is(equalTo(identifiers.size())));
    }

    @Test
    @Tag("RemoteTest")
    public void createResourcesReturnsOneResourcePerPublicationInRemoteFolder() {
        publicationImporter = new PublicationImporter(remoteConnection(), EXISTING_DATA_PATH);
        List<Publication> publications = publicationImporter.getPublications();
        List<Resource> resources = publicationImporter.createResources(publications);
        assertThat(resources.size(), is(equalTo(publications.size())));
    }

    @Test
    public void createResourcesReturnsOneResourcePerPublication() {
        List<Publication> publications = publicationImporter.getPublications();
        List<Resource> resources = publicationImporter.createResources(publications);
        assertThat(resources.size(), is(equalTo(EXPECTED_PUBLICATIONS.size())));
    }

    @Test
    public void publicationImporterReturnsListOfLatestVersionsOfPublicationsWhenPathContainsPublications() {
        List<Publication> publications = publicationImporter.getPublications();

        Publication[] expectedPublications = EXPECTED_PUBLICATIONS.toArray(new Publication[0]);
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

    private static List<Publication> constructExpectedPublications() {

        return allSamplePublications()
                   .collect(Collectors.groupingBy(Publication::getIdentifier))
                   .values()
                   .stream()
                   .map(PublicationImporterTest::chooseLatestPublication)
                   .collect(Collectors.toList());
    }

    private static Stream<Publication> allSamplePublications() {
        return Stream.concat(PublicationImporterTest.FIRST_PUBLICATIONS_FILE.stream(),
            PublicationImporterTest.SECOND_PUBLICATIONS_FILE.stream());
    }

    private static Publication chooseLatestPublication(List<Publication> list) {
        return list.stream().reduce(PublicationImporterTest::latest).orElseThrow();
    }

    private static Publication latest(Publication left, Publication right) {
        if (left.getModifiedDate().isAfter(right.getModifiedDate())) {
            return left;
        }
        return right;
    }

    private static List<Publication> samplePublications() {
        var publicationWithDuplicate = PublicationGenerator.publicationWithIdentifier();
        var duplicate = duplicateWithLaterModifiedDate(publicationWithDuplicate);
        var publicationWithoutDuplicate = PublicationGenerator.publicationWithIdentifier();
        return List.of(publicationWithoutDuplicate, duplicate, publicationWithoutDuplicate);
    }

    private static Publication duplicateWithLaterModifiedDate(Publication publicationWithDuplicate) {
        return publicationWithDuplicate.copy().withModifiedDate(laterModifiedDate(publicationWithDuplicate)).build();
    }

    private static Instant laterModifiedDate(Publication publicationWithDuplicate) {
        return publicationWithDuplicate.getModifiedDate().plus(10, ChronoUnit.DAYS);
    }

    private S3Driver remoteConnection() {
        return attempt(() -> new S3Driver(S3Client.create(), EXISTING_REMOTE_BUCKET_NAME))
                   .orElse(fail -> null);
    }

    private static class FakeS3Driver extends S3Driver {

        public FakeS3Driver() {
            super(null, null);
        }

        @Override
        public List<String> getFiles(Path path) {
            return List.of(FIRST_PUBLICATIONS_FILE, SECOND_PUBLICATIONS_FILE)
                       .stream()
                       .flatMap(publications -> serializePublicationsBatch(publications).stream())
                       .map(this::convertJsonToIon)
                       .collect(Collectors.toList());
        }

        public List<String> serializePublicationsBatch(List<Publication> firstPublicationsFile) {

            return firstPublicationsFile.stream()
                       .map(attempt(publication -> objectMapper.convertValue(publication, JsonNode.class)))
                       .map(attempt -> attempt.map(this::addParentItem))
                       .map(attempt -> attempt.map(objectMapper::writeValueAsString))
                       .map(Try::orElseThrow)
                       .collect(Collectors.toList());
        }

        private JsonNode addParentItem(JsonNode json) {
            ObjectNode root = objectMapper.createObjectNode();
            root.set(PublicationImporter.ION_ITEM, json);
            return root;
        }

        private String convertJsonToIon(String json) {
            var ionReader = IonReaderBuilder.standard().build(json);
            var type = ionReader.next();
            ionReader = IonReaderBuilder.standard().build(json);
            var stringWriter = new StringBuilder();
            var ionWriter = IonTextWriterBuilder.standard().build(stringWriter);
            try {
                ionWriter.writeValues(ionReader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return stringWriter.toString();
        }
    }
}