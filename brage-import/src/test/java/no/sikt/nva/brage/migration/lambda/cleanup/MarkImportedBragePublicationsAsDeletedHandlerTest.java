package no.sikt.nva.brage.migration.lambda.cleanup;

import static no.sikt.nva.brage.migration.lambda.cleanup.ListImportedBragePublicationsHandlerTest.EMPTY_SUBTOPIC;
import static no.sikt.nva.brage.migration.lambda.cleanup.ListImportedBragePublicationsHandlerTest.HARDCODED_PATH;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.FILENAME_EMISSION_EVENT_TOPIC;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.sikt.nva.brage.migration.testutils.FakeResourceService;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class MarkImportedBragePublicationsAsDeletedHandlerTest extends ResourcesLocalTest {

    public static final Context context = mock(Context.class);
    private static final Instant NOW = Instant.now();
    private final String bucketName = "some_bucket";
    private FakeResourceService resourceService;
    private MarkImportedBragePublicationsAsDeletedHandler handler;
    private FakeS3Client s3Client;

    @BeforeEach
    public void init() {
        super.init();
        this.resourceService = new FakeResourceService();
        this.s3Client = new FakeS3Client();
        this.handler = new MarkImportedBragePublicationsAsDeletedHandler(resourceService, s3Client);
    }

    @Test
    void shouldMarkAllPublicationFromS3LocationAsDeleted() {
        var publications = createPersistedPublications();
        putIdentifiersToExpectedS3Location(publications);
        var expectedPublications = updateStatusToDeleted(publications);
        var importRequest = new EventReference(FILENAME_EMISSION_EVENT_TOPIC, EMPTY_SUBTOPIC,
                                               URI.create("s3://brage-migration-reports-750639270376"), NOW);
        handler.handleRequest(toJsonStream(importRequest), context);
        var actualPublications = getPublications(publications);
        assertThat(expectedPublications, containsInAnyOrder(actualPublications.toArray()));
    }

    private static Publication updatePublicationsStatusToDeleted(Publication publication) {
        return publication.copy().withStatus(PublicationStatus.DELETED).build();
    }

    private void putIdentifiersToExpectedS3Location(List<Publication> publications) {
        publications.stream()
            .map(publication -> publication.getIdentifier().toString())
            .forEach(object -> s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(HARDCODED_PATH + object).build(),
                RequestBody.empty()));
    }

    private List<Publication> getPublications(List<Publication> publications) {
        return publications.stream()
                   .map(publication -> resourceService.getPublicationByIdentifier(publication.getIdentifier()))
                   .collect(Collectors.toList());
    }

    private <T> InputStream toJsonStream(T importRequest) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(importRequest))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }

    private List<Publication> updateStatusToDeleted(List<Publication> publications) {
        return publications.stream()
                   .map(MarkImportedBragePublicationsAsDeletedHandlerTest::updatePublicationsStatusToDeleted)
                   .collect(Collectors.toList());
    }

    private List<Publication> createPersistedPublications() {
        var publications = IntStream.range(0, 5).boxed()
                               .map(index -> randomPublication()).collect(Collectors.toList());
        publications.forEach(this::persist);
        return publications;
    }

    private void persist(Publication publication) {
        resourceService.addPublicationWithCristinIdentifier(publication);
    }
}
