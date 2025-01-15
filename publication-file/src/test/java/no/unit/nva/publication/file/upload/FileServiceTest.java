package no.unit.nva.publication.file.upload;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileServiceTest extends ResourcesLocalTest {

    private ResourceService resourceService;
    private FileService fileService;
    private CustomerApiClient customerApiClient;
    private AmazonS3Client s3client;

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
        var resource = Resource.fromPublication(publication).persistNew(resourceService, UserInstance.fromPublication(publication));
        var customerId = randomUri();
        var uploadRequest = randomUploadRequest();

        when(customerApiClient.fetch(customerId)).thenReturn(new Customer(Set.of(), null, null));

        assertThrows(ForbiddenException.class,
                     () -> fileService.initiateMultipartUpload(resource.getIdentifier(), customerId, uploadRequest));
    }

    @Test
    void shouldInitiateMultipartUpload() throws ForbiddenException, NotFoundException, BadRequestException {
        var publication = randomPublication();
        var resource = Resource.fromPublication(publication).persistNew(resourceService, UserInstance.fromPublication(publication));
        var customerId = randomUri();
        var uploadRequest = randomUploadRequest();
        var instanceType = publication.getEntityDescription().getReference().getPublicationInstance().getInstanceType();

        when(customerApiClient.fetch(customerId)).thenReturn(new Customer(Set.of(instanceType), null, null));
        when(s3client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(uploadResult());

        var uploadResponse = fileService.initiateMultipartUpload(resource.getIdentifier(), customerId, uploadRequest);

        assertNotNull(uploadResponse.getKey());
    }

    protected InitiateMultipartUploadResult uploadResult() {
        var uploadResult = new InitiateMultipartUploadResult();
        uploadResult.setKey(randomString());
        uploadResult.setUploadId(randomString());
        return uploadResult;
    }

    private static CreateUploadRequestBody randomUploadRequest() {
        return new CreateUploadRequestBody(randomString(), randomString(), randomString());
    }
}