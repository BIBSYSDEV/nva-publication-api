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
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Reference;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CompleteUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class FileService {

    private static final String FILE_NAME_REGEX = "filename=\"(.*)\"";
    private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found!";
    private static final String FILE_NOT_FOUND_MESSAGE = "File not found!";
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

        var resource = fetchResource(resourceIdentifier);

        var customer = customerApiClient.fetch(customerId);
        if (customerDoesNotAllowUploadingFile(customer, resource)) {
            throw new ForbiddenException();
        }

        var request = createUploadRequestBody.toInitiateMultipartUploadRequest(BUCKET_NAME);

        return amazonS3.initiateMultipartUpload(request);
    }

    public PendingOpenFile completeMultipartUpload(SortableIdentifier resourceIdentifier,
                                                   CompleteUploadRequestBody completeUploadRequestBody,
                                                   UserInstance userInstance) throws NotFoundException {

        var resource = fetchResource(resourceIdentifier);

        var completeMultipartUploadRequest = completeUploadRequestBody.toCompleteMultipartUploadRequest(BUCKET_NAME);
        var completeMultipartUploadResult = amazonS3.completeMultipartUpload(completeMultipartUploadRequest);
        var s3ObjectKey = completeMultipartUploadResult.getKey();
        var objectMetadata = getObjectMetadata(s3ObjectKey);

        var file = constructUploadedFile(UUID.fromString(s3ObjectKey), objectMetadata, userInstance);

        FileEntry.create(file, resource.getIdentifier(), userInstance).persist(resourceService);

        return file;
    }

    public void deleteFile(UUID fileIdentifier, SortableIdentifier resourceIdentifier, UserInstance userInstance)
        throws ForbiddenException {
        var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService);

        if (resource.isPresent()) {
            validateDeletePermissions(userInstance, resource.get());

            FileEntry.queryObject(fileIdentifier, resourceIdentifier)
                .fetch(resourceService)
                .ifPresent(resourceService::deleteFile);
        }
    }

    private static void validateDeletePermissions(UserInstance userInstance, Resource resource)
        throws ForbiddenException {
        if (!PublicationPermissions.create(resource.toPublication(), userInstance)
                 .allowsAction(PublicationOperation.UPDATE)) {
            throw new ForbiddenException();
        }
    }

    private static String toFileName(String contentDisposition) {
        var pattern = Pattern.compile(FILE_NAME_REGEX);
        var matcher = pattern.matcher(contentDisposition);
        return matcher.matches() ? matcher.group(1) : contentDisposition;
    }

    private static UserUploadDetails createUploadDetails(UserInstance userInstance) {
        return new UserUploadDetails(new Username(userInstance.getUsername()), Instant.now());
    }

    private Resource fetchResource(SortableIdentifier resourceIdentifier) throws NotFoundException {
        return Resource.resourceQueryObject(resourceIdentifier)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
    }

    private ObjectMetadata getObjectMetadata(String key) {
        return amazonS3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, key));
    }

    private PendingOpenFile constructUploadedFile(UUID identifier, ObjectMetadata metadata, UserInstance userInstance) {
        return new PendingOpenFile(identifier, toFileName(metadata.getContentDisposition()), metadata.getContentType(),
                                   metadata.getContentLength(), null, null, null,
                                   getRrs(userInstance.getCustomerId()),
                                   null, createUploadDetails(userInstance));
    }

    private CustomerRightsRetentionStrategy getRrs(URI customerId) {
        return Optional.ofNullable(customerApiClient.fetch(customerId))
                   .map(Customer::getRightsRetentionStrategy)
                   .map(CustomerApiRightsRetention::getType)
                   .map(RightsRetentionStrategyConfiguration::fromValue)
                   .map(CustomerRightsRetentionStrategy::create)
                   .orElse(null);
    }

    private boolean customerDoesNotAllowUploadingFile(Customer customer, Resource resource) {
        var instanceType = Optional.ofNullable(resource.getEntityDescription())
                               .map(EntityDescription::getReference)
                               .map(Reference::getPublicationInstance)
                               .map(PublicationInstance::getInstanceType);
        return instanceType.isPresent() && !customer.getAllowFileUploadForTypes().contains(instanceType.get());
    }

    public void updateFile(UUID fileIdentifier, SortableIdentifier resourceIdentifier, UserInstance userInstance,
                           File file) throws ForbiddenException, NotFoundException {

        var resource = Resource.resourceQueryObject(resourceIdentifier)
                           .fetch(resourceService)
                           .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));

        validateUpdateFilePermissions(resource, userInstance);

        var fileEntry = FileEntry.queryObject(fileIdentifier, resourceIdentifier)
                            .fetch(resourceService)
                            .orElseThrow(() -> new NotFoundException(FILE_NOT_FOUND_MESSAGE));

        fileEntry.update(file, resourceService);
    }

    private static void validateUpdateFilePermissions(Resource resource, UserInstance userInstance)
        throws ForbiddenException {
        if (!PublicationPermissions.create(resource.toPublication(), userInstance)
                 .allowsAction(PublicationOperation.UPDATE)) {
            throw new ForbiddenException();
        }
    }
}
