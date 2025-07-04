package no.unit.nva.publication.file.upload;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomHiddenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.publication.model.business.UserInstanceFixture.getDegreeAndFileCuratorFromPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingInternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.RejectedFile;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.ExternalCompleteUploadRequest;
import no.unit.nva.publication.file.upload.restmodel.InternalCompleteUploadRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstanceFixture;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class FileServiceTest extends ResourcesLocalTest {

    public static final String FILE_NAME = randomString();
    public static final String CONTENT_TYPE = "application/pdf";
    public static final int CONTENT_LENGTH = 12345;
    private ResourceService resourceService;
    private FileService fileService;
    private CustomerApiClient customerApiClient;
    private AmazonS3Client s3client;

    public static Stream<Arguments> invalidFileConversionsProvider() {
        return Stream.of(Arguments.of(PendingOpenFile.class, OpenFile.class),
                         Arguments.of(PendingOpenFile.class, InternalFile.class),
                         Arguments.of(PendingOpenFile.class, RejectedFile.class),
                         Arguments.of(PendingOpenFile.class, UploadedFile.class),

                         Arguments.of(PendingInternalFile.class, OpenFile.class),
                         Arguments.of(PendingInternalFile.class, InternalFile.class),
                         Arguments.of(PendingInternalFile.class, RejectedFile.class),
                         Arguments.of(PendingInternalFile.class, UploadedFile.class),

                         Arguments.of(HiddenFile.class, OpenFile.class),
                         Arguments.of(HiddenFile.class, InternalFile.class),
                         Arguments.of(HiddenFile.class, RejectedFile.class),
                         Arguments.of(HiddenFile.class, UploadedFile.class),

                         Arguments.of(RejectedFile.class, OpenFile.class),
                         Arguments.of(RejectedFile.class, InternalFile.class),
                         Arguments.of(RejectedFile.class, UploadedFile.class),

                         Arguments.of(UploadedFile.class, OpenFile.class),
                         Arguments.of(UploadedFile.class, InternalFile.class),
                         Arguments.of(UploadedFile.class, RejectedFile.class),

                         Arguments.of(OpenFile.class, InternalFile.class),
                         Arguments.of(OpenFile.class, RejectedFile.class),
                         Arguments.of(OpenFile.class, UploadedFile.class),

                         Arguments.of(InternalFile.class, OpenFile.class),
                         Arguments.of(InternalFile.class, RejectedFile.class),
                         Arguments.of(InternalFile.class, UploadedFile.class)

        );
    }

    public static Stream<Arguments> validFileConversionsProvider() {
        return Stream.of(Arguments.of(PendingOpenFile.class, PendingOpenFile.class),
                         Arguments.of(PendingOpenFile.class, PendingInternalFile.class),
                         Arguments.of(PendingOpenFile.class, HiddenFile.class),

                         Arguments.of(PendingInternalFile.class, PendingOpenFile.class),
                         Arguments.of(PendingInternalFile.class, PendingInternalFile.class),
                         Arguments.of(PendingInternalFile.class, HiddenFile.class),

                         Arguments.of(HiddenFile.class, PendingOpenFile.class),
                         Arguments.of(HiddenFile.class, PendingInternalFile.class),
                         Arguments.of(HiddenFile.class, HiddenFile.class),

                         Arguments.of(RejectedFile.class, PendingOpenFile.class),
                         Arguments.of(RejectedFile.class, PendingInternalFile.class),
                         Arguments.of(RejectedFile.class, HiddenFile.class),

                         Arguments.of(UploadedFile.class, PendingOpenFile.class),
                         Arguments.of(UploadedFile.class, PendingInternalFile.class),
                         Arguments.of(UploadedFile.class, HiddenFile.class),

                         Arguments.of(OpenFile.class, PendingOpenFile.class),
                         Arguments.of(OpenFile.class, PendingInternalFile.class),
                         Arguments.of(OpenFile.class, HiddenFile.class),

                         Arguments.of(InternalFile.class, PendingOpenFile.class),
                         Arguments.of(InternalFile.class, PendingInternalFile.class),
                         Arguments.of(InternalFile.class, HiddenFile.class));
    }

    public static Stream<Arguments> fileTypeProvider() {
        return Stream.of(Arguments.of(OpenFile.class, "OpenFile"), Arguments.of(InternalFile.class, "InternalFile"));
    }

    protected InitiateMultipartUploadResult uploadResult() {
        var uploadResult = new InitiateMultipartUploadResult();
        uploadResult.setKey(randomString());
        uploadResult.setUploadId(randomString());
        return uploadResult;
    }

    @BeforeEach
    void setUp() {
        super.init();
        s3client = mock(AmazonS3Client.class);
        customerApiClient = mock(CustomerApiClient.class);
        resourceService = getResourceService(client);
        fileService = new FileService(s3client, customerApiClient, resourceService);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenInitiatingMultipartUploadForFileWithoutPublication() {
        var resourceIdentifier = SortableIdentifier.next();
        var userInstance = UserInstance.create(new User(randomString()), randomUri());
        var uploadRequest = randomUploadRequest();

        assertThrows(NotFoundException.class,
                     () -> fileService.initiateMultipartUpload(resourceIdentifier, userInstance, uploadRequest));
    }

    @Test
    void shouldThrowForbiddenExceptionWhenInitiationMultipartUploadAndUserIsNotAllowedToUploadFileForPublication()
        throws BadRequestException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        var customerId = randomUri();
        var userInstance = UserInstance.create(new User(randomString()), customerId);
        var uploadRequest = randomUploadRequest();

        assertThrows(ForbiddenException.class,
                     () -> fileService.initiateMultipartUpload(resource.getIdentifier(), userInstance, uploadRequest));
    }

    @Test
    void shouldInitiateMultipartUpload() throws NotFoundException, BadRequestException, ForbiddenException {
        var publication = randomPublication();
        UserInstance owner = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, owner);
        var uploadRequest = randomUploadRequest();
        var instanceType = publication.getEntityDescription().getReference().getPublicationInstance().getInstanceType();
        when(customerApiClient.fetch(owner.getCustomerId())).thenReturn(new Customer(Set.of(instanceType), null, null));
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(uploadResult());

        var uploadResponse = fileService.initiateMultipartUpload(resource.getIdentifier(), owner, uploadRequest);

        assertNotNull(uploadResponse.getKey());
    }

    @Test
    void shouldInitiateMultipartUploadForExternalClientWithoutValidatingCustomerConfig()
        throws NotFoundException, BadRequestException, ForbiddenException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        var uploadRequest = randomUploadRequest();
        var userInstance = externalUserInstance(resource);

        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(uploadResult());
        var uploadResponse = fileService.initiateMultipartUpload(resource.getIdentifier(), userInstance, uploadRequest);
        verify(customerApiClient, never()).fetch(any());

        assertNotNull(uploadResponse.getKey());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUpdatingNonExistingFile() {
        assertThrows(NotFoundException.class,
                     () -> fileService.updateFile(UUID.randomUUID(), SortableIdentifier.next(), null, null));
    }

    @Test
    void shouldThrowForbiddenWhenUserHasNoPermissionToUpdateFile() throws BadRequestException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        var userInstance = UserInstance.create(new User(randomString()), randomUri());
        var file = randomHiddenFile();
        FileEntry.create(file, resource.getIdentifier(), userInstance).persist(resourceService);

        assertThrows(ForbiddenException.class,
                     () -> fileService.updateFile(file.getIdentifier(), resource.getIdentifier(), userInstance, file));
    }

    @Test
    void shouldThrowNotFoundWhenFileDoesNotExist() throws BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);

        assertThrows(NotFoundException.class,
                     () -> fileService.updateFile(UUID.randomUUID(), resource.getIdentifier(), userInstance, null));
    }

    @Test
    void shouldUpdateMutableFileFields() throws BadRequestException, ForbiddenException, NotFoundException {
        var publication = randomPublication();
        var curator = getDegreeAndFileCuratorFromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, curator);

        var file = randomHiddenFile();
        FileEntry.create(file, resource.getIdentifier(), curator).persist(resourceService);

        var updatedFile = file.copy()
                              .withLicense(randomUri())
                              .withEmbargoDate(Instant.now())
                              .withLegalNote(randomString())
                              .withPublisherVersion(PublisherVersion.ACCEPTED_VERSION)
                              .buildHiddenFile();

        fileService.updateFile(file.getIdentifier(), resource.getIdentifier(), curator, updatedFile);

        var fetchedFile = FileEntry.queryObject(file.getIdentifier(), resource.getIdentifier())
                              .fetch(resourceService)
                              .orElseThrow()
                              .getFile();

        assertEquals(updatedFile, fetchedFile);
    }

    @Test
    void shouldUpdateFileTypeWhenAllowed() throws BadRequestException, ForbiddenException, NotFoundException {
        var publication = randomPublication();
        var curator = getDegreeAndFileCuratorFromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, curator);

        var file = randomPendingInternalFile();
        FileEntry.create(file, resource.getIdentifier(), curator).persist(resourceService);

        var updatedFile = file.toPendingOpenFile();

        fileService.updateFile(file.getIdentifier(), resource.getIdentifier(), curator, updatedFile);

        var fetchedFile = FileEntry.queryObject(file.getIdentifier(), resource.getIdentifier())
                              .fetch(resourceService)
                              .orElseThrow()
                              .getFile();

        assertEquals(updatedFile, fetchedFile);
    }

    @ParameterizedTest
    @MethodSource("invalidFileConversionsProvider")
    void shouldNotAllowFileTypeConversions(Class<? extends File> clazz, Class<? extends File> updatedClazz)
        throws BadRequestException {
        var publication =randomPublication();
        var curator = UserInstanceFixture.getDegreeAndFileCuratorFromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, curator);

        var originalFile = randomHiddenFile().copy().build(clazz);
        FileEntry.create(originalFile, resource.getIdentifier(), curator).persist(resourceService);

        var updatedFile = originalFile.copy().build(updatedClazz);

        assertThrows(IllegalStateException.class,
                     () -> fileService.updateFile(originalFile.getIdentifier(), resource.getIdentifier(), curator,
                                                  updatedFile));
    }

    @ParameterizedTest
    @MethodSource("validFileConversionsProvider")
    void shouldAllowFileTypeConversions(Class<? extends File> clazz, Class<? extends File> updatedClazz)
        throws BadRequestException {
        var publication = randomPublication();
        var curator = getDegreeAndFileCuratorFromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, curator);

        var originalFile = randomHiddenFile().copy().build(clazz);
        FileEntry.create(originalFile, resource.getIdentifier(), curator).persist(resourceService);

        var updatedFile = originalFile.copy().build(updatedClazz);

        assertDoesNotThrow(
            () -> fileService.updateFile(originalFile.getIdentifier(), resource.getIdentifier(), curator,
                                         updatedFile));
    }

    @Test
    void shouldIgnoreImmutableFileFieldsWhenUpdatingFile()
        throws BadRequestException, ForbiddenException, NotFoundException {
        var publication = randomPublication();
        var curator = getDegreeAndFileCuratorFromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, curator);

        var originalFile = randomHiddenFile();
        FileEntry.create(originalFile, resource.getIdentifier(), curator).persist(resourceService);

        var updatedFile = originalFile.copy().withIdentifier(UUID.randomUUID()).buildHiddenFile();

        fileService.updateFile(originalFile.getIdentifier(), resource.getIdentifier(), curator, updatedFile);

        var fetchedFile = FileEntry.queryObject(originalFile.getIdentifier(), resource.getIdentifier())
                              .fetch(resourceService)
                              .orElseThrow()
                              .getFile();

        assertEquals(originalFile.getIdentifier(), fetchedFile.getIdentifier());
        Assertions.assertNotEquals(updatedFile.getIdentifier(), fetchedFile.getIdentifier());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenPersistingFileForNotExistingPublication() {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        mockCompleteMultipartUpload();
        var request = new InternalCompleteUploadRequest(randomString(), randomString(), List.of());

        assertThrows(NotFoundException.class,
                     () -> fileService.completeMultipartUpload(publication.getIdentifier(), request, userInstance));
    }

    @Test
    void shouldPersistUploadedFileEntryInDatabaseWhenCompletingMultipartUpload()
        throws BadRequestException, NotFoundException, ForbiddenException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var completeMultipartUploadResult = mockCompleteMultipartUpload();
        var request = new InternalCompleteUploadRequest(randomString(), randomString(), List.of());

        mockCustomerResponse(userInstance);
        fileService.completeMultipartUpload(resource.getIdentifier(), request, userInstance);

        var fileEntry = FileEntry.queryObject(UUID.fromString(completeMultipartUploadResult.getKey()),
                                              resource.getIdentifier()).fetch(resourceService).orElseThrow();

        var expectedFile = constructExpectedFile(completeMultipartUploadResult, userInstance, fileEntry);

        assertEquals(expectedFile, fileEntry.getFile());
    }

    @ParameterizedTest
    @MethodSource("fileTypeProvider")
    void shouldPersistRequestedFinalizedFileEntryInDatabaseWhenCompletingMultipartUploadAsExternalClient(
        Class<? extends File> expectedFileClass, String fileType)
        throws BadRequestException, NotFoundException, ForbiddenException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        var completeMultipartUploadResult = mockCompleteMultipartUpload();
        var request = new ExternalCompleteUploadRequest(randomString(), randomString(), List.of(), fileType, null, null,
                                                        null);
        var userInstance = constructExternalClient(publication);
        fileService.completeMultipartUpload(resource.getIdentifier(), request, userInstance);

        var fileEntry = FileEntry.queryObject(UUID.fromString(completeMultipartUploadResult.getKey()),
                                              resource.getIdentifier()).fetch(resourceService).orElseThrow();

        assertEquals(request.license(), fileEntry.getFile().getLicense());
        assertEquals(request.publisherVersion(), fileEntry.getFile().getPublisherVersion());
        assertEquals(request.embargoDate(), fileEntry.getFile().getEmbargoDate().orElse(null));
        assertInstanceOf(expectedFileClass, fileEntry.getFile());
    }

    @Test
    void shouldThrowForbiddenWhenAttemptingToPersistNotSupportedFileTypeAsExternalClient() throws BadRequestException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        mockCompleteMultipartUpload();
        var request = new ExternalCompleteUploadRequest(randomString(), randomString(), List.of(), "PendingOpenFile",
                                                        null, null, null);
        var userInstance = constructExternalClient(publication);

        assertThrows(BadRequestException.class,
                     () -> fileService.completeMultipartUpload(resource.getIdentifier(), request, userInstance));
    }

    @Test
    void shouldSetNullRrsWhenNullRrsAtCustomer()
        throws BadRequestException, NotFoundException, ForbiddenException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var completeMultipartUploadResult = mockCompleteMultipartUpload();
        var request = new InternalCompleteUploadRequest(randomString(), randomString(), List.of());

        mockCustomerResponseWithNullRrs(userInstance);
        fileService.completeMultipartUpload(resource.getIdentifier(), request, userInstance);

        var fileEntry = FileEntry.queryObject(UUID.fromString(completeMultipartUploadResult.getKey()),
                                              resource.getIdentifier()).fetch(resourceService).orElseThrow();

        assertEquals(fileEntry.getFile().getRightsRetentionStrategy(),
                     NullRightsRetentionStrategy.create(NULL_RIGHTS_RETENTION_STRATEGY));
    }

    private static UserInstance constructExternalClient(Publication publication) {
        return new UserInstance(randomString(), publication.getPublisher().getId(), randomUri(), randomUri(), randomUri(),
                                List.of(),
                                UserClientType.EXTERNAL);
    }

    private static File constructExpectedFile(CompleteMultipartUploadResult completeMultipartUploadResult,
                                              UserInstance userInstance, FileEntry fileEntry) {
        return new UploadedFile(UUID.fromString(completeMultipartUploadResult.getKey()), FILE_NAME, CONTENT_TYPE,
                                (long) CONTENT_LENGTH,
                                CustomerRightsRetentionStrategy.create(RIGHTS_RETENTION_STRATEGY),
                                new UserUploadDetails(new Username(userInstance.getUsername()),
                                                      fileEntry.getFile().getUploadDetails().uploadedDate()));
    }

    private static CreateUploadRequestBody randomUploadRequest() {
        return new CreateUploadRequestBody(randomString(), randomString(), randomString());
    }

    private UserInstance externalUserInstance(Publication resource) {
        return new UserInstance(randomString(), resource.getPublisher().getId(), randomUri(), randomUri(), randomUri(),
                                List.of(),
                                UserClientType.EXTERNAL);
    }

    private void mockCustomerResponse(UserInstance userInstance) {
        when(customerApiClient.fetch(userInstance.getCustomerId())).thenReturn(new Customer(null, null,
                                                                                            new CustomerApiRightsRetention(
                                                                                                RIGHTS_RETENTION_STRATEGY.getValue(),
                                                                                                randomString())));
    }

    private void mockCustomerResponseWithNullRrs(UserInstance userInstance) {
        when(customerApiClient.fetch(userInstance.getCustomerId())).thenReturn(new Customer(null, null,
                                                                                            new CustomerApiRightsRetention(
                                                                                                NULL_RIGHTS_RETENTION_STRATEGY.getValue(),
                                                                                                randomString())));
    }

    private CompleteMultipartUploadResult mockCompleteMultipartUpload() {
        var completeMultipartUploadResult = new CompleteMultipartUploadResult();
        completeMultipartUploadResult.setKey(UUID.randomUUID().toString());
        when(s3client.completeMultipartUpload(Mockito.any(CompleteMultipartUploadRequest.class))).thenReturn(
            completeMultipartUploadResult);
        var s3object = new S3Object();
        s3object.setKey(randomString());
        var metadata = new ObjectMetadata();
        metadata.setContentLength(CONTENT_LENGTH);
        metadata.setContentDisposition(FILE_NAME);
        metadata.setContentType(CONTENT_TYPE);
        s3object.setObjectMetadata(metadata);
        when(s3client.getObjectMetadata(any())).thenReturn(metadata);
        return completeMultipartUploadResult;
    }
}