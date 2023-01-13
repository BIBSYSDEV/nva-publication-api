package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.MULTIPLE_PARENT_PUBLICATIONS_INFORMATION;
import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.NO_PARENT_PUBLICATION_FOUND_EXCEPTION;
import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.SUBTOPIC;
import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.TOPIC;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import no.unit.nva.cristin.mapper.NvaPublicationPartOf;
import no.unit.nva.cristin.mapper.NvaPublicationPartOfCristinPublication;
import no.unit.nva.cristin.mapper.nva.exceptions.ParentPublicationException;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class CristinPatchEventConsumerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = mock(Context.class);

    private FakeS3Client s3Client;
    private S3Driver s3Driver;

    private CristinPatchEventConsumer handler;

    private ResourceService resourceService;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = new ResourceService(super.client, Clock.systemDefaultZone());
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignored");
        handler = new CristinPatchEventConsumer(resourceService, s3Client);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldThrowExceptionWhenPublicationCannotBeRetrieved() throws ApiGatewayException, IOException {
        var partOfCristinId = randomString();
        var childPublicationIdentifier = SortableIdentifier.next();
        createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(partOfCristinId);
        var partOfEventReference = createPartOfEventReference(
            childPublicationIdentifier.toString(),
            partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var input = toInputStream(eventReference);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldThrowExceptionWhenSearchingForNvaPublicationByCristinIdentifierReturnsMoreThanOnePublication()
        throws ApiGatewayException, IOException {
        var childPublication = createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString());
        var partOfCristinId = randomString();
        persistSeveralPublicationsWithTheSameCristinId(partOfCristinId);
        var partOfEventReference = createPartOfEventReference(
            childPublication.getIdentifier().toString(),
            partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var input = toInputStream(eventReference);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        var exception = assertThrows(ParentPublicationException.class, action);
        assertThat(exception.getMessage(), containsString(MULTIPLE_PARENT_PUBLICATIONS_INFORMATION));
    }

    @Test
    void shouldThrowExceptionWhenSearchingForNvaPublicationByCristinIdentifierReturnsNoPublication()
        throws ApiGatewayException, IOException {
        var childPublication = createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString());
        var partOfCristinId = randomString();
        var partOfEventReference = createPartOfEventReference(
            childPublication.getIdentifier().toString(),
            partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var input = toInputStream(eventReference);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        var exception = assertThrows(ParentPublicationException.class, action);
        assertThat(exception.getMessage(), containsString(NO_PARENT_PUBLICATION_FOUND_EXCEPTION));
    }

    @Test
    void shouldThrowExceptionWhenSubtopicIsNotAsExpected() throws IOException {
        var fileUri = s3Driver.insertFile(randomPath(), randomString());
        var eventReferenceWithInvalidSubtopic = createEventWithInvalidSubtopic(fileUri);
        var input = toInputStream(eventReferenceWithInvalidSubtopic);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void shouldThrowExceptionWhenTopicIsNotAsExpected() throws IOException {
        var fileUri = s3Driver.insertFile(randomPath(), randomString());
        var eventReferenceWithInvalidTopic = createEventWithInvalidTopic(fileUri);
        var input = toInputStream(eventReferenceWithInvalidTopic);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        assertThrows(IllegalArgumentException.class, action);
    }

    private static AwsEventBridgeEvent<EventReference> createEventWithInvalidTopic(URI fileUri) {
        var eventReference = new EventReference(randomString(), SUBTOPIC, fileUri);
        var request = new AwsEventBridgeEvent<EventReference>();

        request.setDetail(eventReference);
        return request;
    }

    private void persistSeveralPublicationsWithTheSameCristinId(String cristinId) throws ApiGatewayException {
        createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(cristinId);
        createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(cristinId);
    }

    private String createPartOfEventReference(String childPublicationId, String partOfCristinId) {
        var partOf =
            NvaPublicationPartOfCristinPublication.builder()
                .withNvaPublicationIdentifier(childPublicationId)
                .withPartOf(NvaPublicationPartOf.builder().withCristinId(partOfCristinId).build())
                .build();
        return partOf.toJsonString();
    }

    private Publication createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(String cristinId)
        throws ApiGatewayException {
        Publication publication = PublicationGenerator.randomPublication();
        publication.setAdditionalIdentifiers(createAdditionalIdentifiersWithCristinId(cristinId));
        UserInstance userInstance = UserInstance.fromPublication(publication);
        SortableIdentifier publicationIdentifier =
            Resource.fromPublication(publication).persistNew(resourceService, userInstance).getIdentifier();
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Set<AdditionalIdentifier> createAdditionalIdentifiersWithCristinId(String cristinId) {
        return Set.of(new AdditionalIdentifier("Cristin", cristinId));
    }

    private AwsEventBridgeEvent<EventReference> createEventWithInvalidSubtopic(URI fileUri) {
        var eventReference = new EventReference(TOPIC, randomString(), fileUri);
        var request = new AwsEventBridgeEvent<EventReference>();

        request.setDetail(eventReference);
        return request;
    }

    private UnixPath randomPath() {
        return UnixPath.of(randomString(), randomString());
    }

    private InputStream toInputStream(AwsEventBridgeEvent<EventReference> request) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(request))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }

    private AwsEventBridgeEvent<EventReference> createInputEventForFile(URI fileUri) {
        var eventReference = new EventReference(TOPIC,
                                                SUBTOPIC,
                                                fileUri,
                                                Instant.now());
        var request = new AwsEventBridgeEvent<EventReference>();

        request.setDetail(eventReference);
        return request;
    }
}
