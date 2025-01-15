package no.unit.nva.publication.file.upload;

import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CompleteUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class FileService {

    public static final String FILE_NAME_REGEX = "filename=\"(.*)\"";
    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found!";
    private final AmazonS3 amazonS3;
    private final CustomerApiClient customerApiClient;
    private final ResourceService resourceService;

    public FileService(AmazonS3 amazonS3, CustomerApiClient customerApiClient, ResourceService resourceService) {
        this.amazonS3 = amazonS3;
        this.customerApiClient = customerApiClient;
        this.resourceService = resourceService;
    }

    public FileService(AmazonS3 amazonS3, ResourceService resourceService) {
        this.amazonS3 = amazonS3;
        this.customerApiClient = null;
        this.resourceService = resourceService;
    }

    @JacocoGenerated
    public static FileService defaultFileService() {
        return new FileService(AmazonS3ClientBuilder.defaultClient(), JavaHttpClientCustomerApiClient.defaultInstance(),
                               ResourceService.defaultService());
    }

    public static String toFileName(String contentDisposition) {
        var pattern = Pattern.compile(FILE_NAME_REGEX);
        var matcher = pattern.matcher(contentDisposition);
        return matcher.matches() ? matcher.group(1) : contentDisposition;
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

    public UploadedFile completeMultipartUpload(SortableIdentifier resourceIdentifier,
                                                CompleteUploadRequestBody completeUploadRequestBody,
                                                UserInstance userInstance) throws NotFoundException {

        var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService)
            .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));

        var completeMultipartUploadRequest = completeUploadRequestBody.toCompleteMultipartUploadRequest(BUCKET_NAME);
        var completeMultipartUploadResult = amazonS3.completeMultipartUpload(completeMultipartUploadRequest);
        var s3ObjectKey = completeMultipartUploadResult.getKey();
        var objectMetadata = getObjectMetadata(s3ObjectKey);

        var file = constructUploadedFile(s3ObjectKey, objectMetadata, userInstance);

        FileEntry.create(file, resource.getIdentifier(), userInstance).persist(resourceService);

        return file;
    }

    private ObjectMetadata getObjectMetadata(String key) {
        return amazonS3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, key));
    }

    private UploadedFile constructUploadedFile(String identifier, ObjectMetadata objectMetadata,
                                               UserInstance userInstance) {
        return new UploadedFile(UUID.fromString(identifier), toFileName(objectMetadata.getContentDisposition()),
                                objectMetadata.getContentType(), objectMetadata.getContentLength(),
                                new UserUploadDetails(new Username(userInstance.getUsername()), Instant.now()));
    }

    private boolean customerDoesNotAllowUploadingFile(Customer customer, Resource resource) {
        var instanceType = Optional.ofNullable(resource.getEntityDescription())
                               .map(EntityDescription::getReference)
                               .map(Reference::getPublicationInstance)
                               .map(PublicationInstance::getInstanceType);
        return instanceType.isPresent() && !customer.getAllowFileUploadForTypes().contains(instanceType.get());
    }
}
