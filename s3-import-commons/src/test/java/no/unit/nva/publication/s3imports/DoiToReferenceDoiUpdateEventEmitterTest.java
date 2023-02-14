package no.unit.nva.publication.s3imports;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.kms.model.NotFoundException;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import org.hamcrest.core.Every;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


public class DoiToReferenceDoiUpdateEventEmitterTest extends ResourcesLocalTest {
    public static final String DEFAULT_FILE_NAME = "results.csv";
    private static final Context context = mock(Context.class);
    private FakeS3Client s3Client;
    private ResourceService resourceService;
    private DoiToReferenceDoiUpdateEventEmitter handler;

    @BeforeEach
    public void init() {
        super.init();
        this.s3Client = new FakeS3Client();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.handler = new DoiToReferenceDoiUpdateEventEmitter(s3Client, resourceService);
    }

    @Test
    void shouldMoveDoiValueToLinkField() throws IOException, NotFoundException {
        var publicationsToModify = createPublications();
        var eventReference = createEventReference(publicationsToModify);

        var inputStream = toInputStream(eventReference);
        handler.handleRequest(inputStream, new ByteArrayOutputStream(), context);
        var updatedPublications = fetchUpdatedPublications(publicationsToModify);

        var expectedPublications = createPublicationWithDoiValueInLinkField(publicationsToModify);

        assertThat(expectedPublications, (Every.everyItem(hasProperty("doi", is(equalTo(null))))));
        assertThat(expectedPublications, containsInAnyOrder(updatedPublications.toArray()));
    }

    private EventReference createEventReference(List<Publication> publications) {
        s3Client.putObject(
            PutObjectRequest.builder().bucket("nve-doi-to-update-bucket").key(DEFAULT_FILE_NAME).build(),
            RequestBody.fromString(getPublicationInCSVFormat(publications)));
        return new EventReference(null, null, URI.create("s3://nve-doi-to-update-bucket"
                                                         + "/results.csv"));
    }

    private static String getPublicationInCSVFormat(List<Publication> publications) {
        return publications.stream()
                   .map(Publication::getIdentifier)
                   .map(SortableIdentifier::toString)
                   .collect(Collectors.joining("\n"));
    }

    private static Publication moveDoiToNewField(Publication publication) {
        publication.getEntityDescription().getReference().setDoi(publication.getDoi());
        publication.setDoi(null);
        return publication;
    }

    private List<Publication> fetchUpdatedPublications(List<Publication> publicationsToModify) {
        var publications = publicationsToModify.stream()
                               .map(Publication::getIdentifier)
                               .map(identifier -> attempt(
                                   () -> resourceService.getPublicationByIdentifier(identifier)).orElseThrow())
                               .collect(Collectors.toList());
        publications.forEach(publication -> publication.setModifiedDate(null));
        return publications;
    }

    private Publication persist(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        return Resource.fromPublication(publication).persistNew(resourceService, userInstance);
    }

    private List<Publication> createPublicationWithDoiValueInLinkField(List<Publication> publicationsToModify) {
        var publications = publicationsToModify.stream()
                               .map(DoiToReferenceDoiUpdateEventEmitterTest::moveDoiToNewField)
                               .collect(Collectors.toList());
        publications.forEach(i -> i.setModifiedDate(null));
        return publications;
    }

    private InputStream toInputStream(EventReference request) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(request))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }

    private List<Publication> createPublications() {
        return IntStream.range(0, 10)
                   .boxed()
                   .map(i -> persist(randomPublication()))
                   .collect(Collectors.toList());
    }
}
