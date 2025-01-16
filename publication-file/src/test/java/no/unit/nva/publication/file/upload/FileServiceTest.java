package no.unit.nva.publication.file.upload;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CompleteUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FileServiceTest extends ResourcesLocalTest {

    public static final String FILE_NAME = randomString();
    public static final String CONTENT_TYPE = "application/pdf";
    public static final int CONTENT_LENGTH = 12345;
    private ResourceService resourceService;
    private FileService fileService;
    private CustomerApiClient customerApiClient;
    private AmazonS3Client s3client;

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
        resourceService = getResourceServiceBuilder().build();
        fileService = new FileService(s3client, customerApiClient, resourceService);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenInitiatingMultipartUploadForFileWithoutPublication() {
        var resourceIdentifier = SortableIdentifier.next();
        var customerId = randomUri();
        var uploadRequest = randomUploadRequest();

        assertThrows(NotFoundException.class,
                     () -> fileService.initiateMultipartUpload(resourceIdentifier, customerId, uploadRequest));
    }

    @Test
    void shouldThrowForbiddenExceptionWhenInitiationMultipartUploadAndCustomerDoesNotAllowFileForPublicationType()
        throws BadRequestException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        var customerId = randomUri();
        var uploadRequest = randomUploadRequest();

        when(customerApiClient.fetch(customerId)).thenReturn(new Customer(Set.of(), null, null));

        assertThrows(ForbiddenException.class,
                     () -> fileService.initiateMultipartUpload(resource.getIdentifier(), customerId, uploadRequest));
    }

    @Test
    void shouldInitiateMultipartUpload() throws ForbiddenException, NotFoundException, BadRequestException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication)
                           .persistNew(resourceService, UserInstance.fromPublication(publication));
        var customerId = randomUri();
        var uploadRequest = randomUploadRequest();
        var instanceType = publication.getEntityDescription().getReference().getPublicationInstance().getInstanceType();

        when(customerApiClient.fetch(customerId)).thenReturn(new Customer(Set.of(instanceType), null, null));
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(uploadResult());

        var uploadResponse = fileService.initiateMultipartUpload(resource.getIdentifier(), customerId, uploadRequest);

        assertNotNull(uploadResponse.getKey());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenPersistingFileForNotExistingPublication() {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        mockCompleteMultipartUpload();
        var request = new CompleteUploadRequestBody(randomString(), randomString(), List.of());

        assertThrows(NotFoundException.class,
                     () -> fileService.completeMultipartUpload(publication.getIdentifier(), request, userInstance));
    }

    @Test
    void shouldPersistFileEntryInDatabaseWhenCompletingMultipartUpload() throws BadRequestException, NotFoundException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var completeMultipartUploadResult = mockCompleteMultipartUpload();
        var request = new CompleteUploadRequestBody(randomString(), randomString(), List.of());

        fileService.completeMultipartUpload(resource.getIdentifier(), request, userInstance);

        var fileEntry = FileEntry.queryObject(UUID.fromString(completeMultipartUploadResult.getKey()),
                                              resource.getIdentifier()).fetch(resourceService).orElseThrow();

        var expectedFile = constructExpectedFile(completeMultipartUploadResult, userInstance, fileEntry);

        assertEquals(expectedFile, fileEntry.getFile());
    }

    private static UploadedFile constructExpectedFile(CompleteMultipartUploadResult completeMultipartUploadResult,
                                                      UserInstance userInstance, FileEntry fileEntry) {
        return new UploadedFile(UUID.fromString(completeMultipartUploadResult.getKey()), FILE_NAME, CONTENT_TYPE,
                                (long) CONTENT_LENGTH, new UserUploadDetails(new Username(userInstance.getUsername()),
                                                                             fileEntry.getFile()
                                                                                 .getUploadDetails()
                                                                                 .uploadedDate()));
    }

    private static CreateUploadRequestBody randomUploadRequest() {
        return new CreateUploadRequestBody(randomString(), randomString(), randomString());
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