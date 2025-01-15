package no.unit.nva.publication.file.upload;

import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class FileService {

    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found!";
    private final AmazonS3 amazonS3;
    private final CustomerApiClient customerApiClient;
    private final ResourceService resourceService;

    public FileService(AmazonS3 amazonS3, CustomerApiClient customerApiClient, ResourceService resourceService) {
        this.amazonS3 = amazonS3;
        this.customerApiClient = customerApiClient;
        this.resourceService = resourceService;
    }

    @JacocoGenerated
    public static FileService defaultFileService() {
        return new FileService(AmazonS3ClientBuilder.defaultClient(), JavaHttpClientCustomerApiClient.defaultInstance(),
                               ResourceService.defaultService());
    }

    public InitiateMultipartUploadResult initiateMultipartUpload(SortableIdentifier resourceIdentifier, URI customerId,
                                                                 CreateUploadRequestBody createUploadRequestBody)
        throws NotFoundException, ForbiddenException {

        var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService)
                           .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));

        var customer = customerApiClient.fetch(customerId);
        if (customerDoesNotAllowUploadingFile(customer, resource)) {
            throw new ForbiddenException();
        }

        var request = createUploadRequestBody.toInitiateMultipartUploadRequest(BUCKET_NAME);

        return amazonS3.initiateMultipartUpload(request);
    }

    public boolean customerDoesNotAllowUploadingFile(Customer customer, Resource resource) {
        var instanceType = Optional.ofNullable(resource.getEntityDescription())
                               .map(EntityDescription::getReference)
                               .map(Reference::getPublicationInstance)
                               .map(PublicationInstance::getInstanceType);
        return instanceType.isPresent() && !customer.getAllowFileUploadForTypes().contains(instanceType.get());
    }
}
