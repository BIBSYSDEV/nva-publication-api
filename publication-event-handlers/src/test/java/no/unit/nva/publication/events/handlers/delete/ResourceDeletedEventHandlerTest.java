package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.model.OperationType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class ResourceDeletedEventHandlerTest extends ResourcesLocalTest {

    private static final String NOT_RELEVANT = "NotRelevant";
    private static final String RESOURCE_STORAGE_BUCKET_NAME = "ResourceStorageBucket";
    private ResourceDeletedEventHandler handler;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private S3Client s3Client;
    private S3Driver eventsS3Driver;
    private S3Driver resourceStorageS3Driver;
    private Environment environment;
    private final FakeContext fakeContext = new FakeContext();

    @BeforeEach
    void setUp() {
        super.init();
        this.output = new ByteArrayOutputStream();
        this.resourceService = getResourceService(client);
        this.environment = mock(Environment.class);
        when(environment.readEnv("RESOURCE_STORAGE_BUCKET_NAME")).thenReturn(RESOURCE_STORAGE_BUCKET_NAME);

        s3Client = new FakeS3Client();
        eventsS3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
        resourceStorageS3Driver = new S3Driver(s3Client, RESOURCE_STORAGE_BUCKET_NAME);
    }

    @Test
    void shouldThrowExceptionWhenEventReferenceIsNotAvailableOnS3() {
        handler = new ResourceDeletedEventHandler(environment, s3Client, resourceService);

        assertThrows(NoSuchKeyException.class, () -> handler.handleRequest(emptyEvent(), output, fakeContext));
    }

    @Test
    void shouldHandleNoFilesForResource() {
        handler = new ResourceDeletedEventHandler(environment, s3Client, resourceService);

        assertDoesNotThrow(() -> handler.handleRequest(resourceWithoutFiles(randomUri()), output, fakeContext));
    }

    @Test
    void shouldDeleteFilesForResourceWithFilesPresentOnS3() throws IOException {
        handler = new ResourceDeletedEventHandler(environment, s3Client, resourceService);
        var resourceIdentifier = SortableIdentifier.next();
        var context = resourceWithFiles(resourceIdentifier, true, randomUri());
        handler.handleRequest(context.request(), output, fakeContext);

        context.identifiers()
            .forEach(identifier -> {
                assertThrows(NoSuchKeyException.class,
                             () -> resourceStorageS3Driver.getFile(UnixPath.of(identifier.key().toString())));
                assertTrue(FileEntry.queryObject(identifier.fileIdentifier())
                               .fetch(resourceService)
                               .isEmpty());
            });
    }

    @ParameterizedTest(name = "shouldDeleteFilesForResourceWithFiles; files present on s3: {0}")
    @ValueSource(booleans = {true, false})
    void shouldDeleteFilesForResourceWithFiles(boolean filesPresentOnS3) throws IOException {
        handler = new ResourceDeletedEventHandler(environment, s3Client, resourceService);
        var resourceIdentifier = SortableIdentifier.next();
        var context = resourceWithFiles(resourceIdentifier, filesPresentOnS3, randomUri());
        handler.handleRequest(context.request(), output, fakeContext);

        context.identifiers()
            .forEach(identifier -> {
                assertThrows(NoSuchKeyException.class,
                             () -> resourceStorageS3Driver.getFile(UnixPath.of(identifier.key().toString())));
                assertTrue(FileEntry.queryObject(identifier.fileIdentifier())
                               .fetch(resourceService)
                               .isEmpty());
            });
    }

    private record FileIdentifiers(UUID key, SortableIdentifier fileIdentifier) {

    }

    private record RequestContext(InputStream request, List<FileIdentifiers> identifiers) {

    }

    private InputStream emptyEvent() {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(new EventReference(NOT_RELEVANT, randomUri()));
    }

    private InputStream resourceWithoutFiles(URI customerId) throws IOException {
        var resourceIdentifier = SortableIdentifier.next();
        var eventBody = eventBody(
            Resource.emptyResource(new User(randomString()), customerId, resourceIdentifier)
        );
        var blobUri = eventsS3Driver.insertEvent(UnixPath.of(randomString()), eventBody);

        return validEvent(blobUri);
    }

    private RequestContext resourceWithFiles(SortableIdentifier resourceIdentifier,
                                             boolean filesPresentOnS3, URI customerId) throws IOException {

        var fileIdentifiers = new ArrayList<FileIdentifiers>();
        fileIdentifiers.add(persistFileEntry(resourceIdentifier, customerId));
        fileIdentifiers.add(persistFileEntry(resourceIdentifier, customerId));

        if (filesPresentOnS3) {
            for (FileIdentifiers identifiers : fileIdentifiers) {
                attempt(
                    () -> resourceStorageS3Driver.insertFile(UnixPath.of(identifiers.key().toString()),
                                                             "dummy")).orElseThrow();
            }
        }
        var eventBody = eventBody(
            Resource.emptyResource(new User(randomString()), customerId, resourceIdentifier)
        );
        var blobUri = eventsS3Driver.insertEvent(UnixPath.of(randomString()), eventBody);

        return new RequestContext(validEvent(blobUri), fileIdentifiers);
    }

    private FileIdentifiers persistFileEntry(SortableIdentifier resourceIdentifier, URI customerId) {
        var s3Key = UUID.randomUUID();
        var file = new PendingOpenFile(s3Key,
                                       randomString(),
                                       randomString(),
                                       1L,
                                       randomUri(),
                                       PublisherVersion.PUBLISHED_VERSION,
                                       null,
                                       NullRightsRetentionStrategy.create(
                                           RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY),
                                       null,
                                       null);
        var userInstance = UserInstance.create(new User(randomString()), customerId);
        var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);
        resourceService.persistFile(fileEntry);

        return new FileIdentifiers(s3Key, fileEntry.getIdentifier());
    }

    private InputStream validEvent(URI uri) {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(new EventReference(NOT_RELEVANT, uri));
    }

    private String eventBody(Resource oldResource) {
        return new DataEntryUpdateEvent(
            OperationType.REMOVE.name(), oldResource, null)
                   .toJsonString();
    }
}
