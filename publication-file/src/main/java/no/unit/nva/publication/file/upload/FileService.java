package no.unit.nva.publication.file.upload;

import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Reference;
import no.unit.nva.model.associatedartifacts.file.MutableFileMetadata;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.UpdateFileRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class FileService {

    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found!";
    public static final String FILE_NOT_FOUND_MESSAGE = "File not found!";
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

        var resource = Resource.resourceQueryObject(resourceIdentifier)
                           .fetch(resourceService)
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

    public void updateFile(UUID fileIdentifier, SortableIdentifier resourceIdentifier, UserInstance userInstance,
                           MutableFileMetadata mutableFileMetadata) throws ForbiddenException, NotFoundException {

        var resource = Resource.resourceQueryObject(resourceIdentifier)
                           .fetch(resourceService)
                           .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));

        validateUpdateFilePermissions(resource, userInstance);

        var fileEntry = FileEntry.queryObject(fileIdentifier, resourceIdentifier)
                            .fetch(resourceService)
                            .orElseThrow(() -> new NotFoundException(FILE_NOT_FOUND_MESSAGE));

        fileEntry.update(mutableFileMetadata, resourceService);
    }

    private static void validateUpdateFilePermissions(Resource resource, UserInstance userInstance)
        throws ForbiddenException {
        if (!PublicationPermissions.create(resource.toPublication(), userInstance)
                 .allowsAction(PublicationOperation.UPDATE)) {
            throw new ForbiddenException();
        }
    }
}
