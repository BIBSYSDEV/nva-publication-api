package no.unit.nva.publication.file.upload;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerApiRightsRetention;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.CompleteUploadRequest;
import no.unit.nva.publication.file.upload.restmodel.CreateUploadRequestBody;
import no.unit.nva.publication.file.upload.restmodel.ExternalCompleteUploadRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissions;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class FileService {

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

    public InitiateMultipartUploadResult initiateMultipartUpload(SortableIdentifier resourceIdentifier,
                                                                 UserInstance userInstance,
                                                                 CreateUploadRequestBody createUploadRequestBody)
        throws NotFoundException, ForbiddenException {

        var resource = fetchResource(resourceIdentifier);

        validateUploadPermissions(userInstance, resource);

        var request = createUploadRequestBody.toInitiateMultipartUploadRequest(BUCKET_NAME);

        return amazonS3.initiateMultipartUpload(request);
    }

    public File completeMultipartUpload(SortableIdentifier resourceIdentifier,
                                        CompleteUploadRequest request, UserInstance userInstance)
        throws NotFoundException, BadRequestException, ForbiddenException {

        var resource = fetchResource(resourceIdentifier);

        var completeMultipartUploadRequest = request.toCompleteMultipartUploadRequest(BUCKET_NAME);
        var completeMultipartUploadResult = amazonS3.completeMultipartUpload(completeMultipartUploadRequest);
        var s3ObjectKey = completeMultipartUploadResult.getKey();
        var objectMetadata = getObjectMetadata(s3ObjectKey);

        var file = userInstance.isExternalClient() && request instanceof ExternalCompleteUploadRequest externalRequest
                       ? constructFileForExternalClient(UUID.fromString(s3ObjectKey), externalRequest, objectMetadata)
                       : constructUploadedFile(UUID.fromString(s3ObjectKey), objectMetadata, userInstance);

        validateUploadPermissions(userInstance, resource);

        FileEntry.create(file, resource.getIdentifier(), userInstance).persist(resourceService);
        return file;
    }

    public void deleteFile(UUID fileIdentifier, SortableIdentifier resourceIdentifier, UserInstance userInstance)
        throws ForbiddenException {
        var resource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService);
        var fileEntry = FileEntry.queryObject(fileIdentifier, resourceIdentifier)
                            .fetch(resourceService);
        if (resource.isPresent() && fileEntry.isPresent()) {
            validateDeletePermissions(userInstance, fileEntry.get(), resource.get());
            fileEntry.get().softDelete(resourceService, userInstance.getUser());
        }
    }

    public File constructFileForExternalClient(UUID identifier,
                                               ExternalCompleteUploadRequest uploadRequest,
                                               ObjectMetadata metadata) throws BadRequestException {
        var builder = File.builder()
                          .withIdentifier(identifier)
                          .withName(Filename.fromContentDispositionValue(metadata.getContentDisposition()))
                          .withSize(metadata.getContentLength())
                          .withMimeType(metadata.getContentType())
                          .withLicense(uploadRequest.license())
                          .withPublisherVersion(uploadRequest.publisherVersion())
                          .withEmbargoDate(uploadRequest.embargoDate());
        return switch (uploadRequest.fileType()) {
            case OpenFile.TYPE:
                yield builder.build(OpenFile.class);
            case InternalFile.TYPE:
                yield builder.build(InternalFile.class);
            default:
                throw new BadRequestException("Unknown file type: " + uploadRequest.fileType());
        };
    }

    public void updateFile(UUID fileIdentifier, SortableIdentifier resourceIdentifier, UserInstance userInstance,
                           File file) throws ForbiddenException, NotFoundException {

        var resource = Resource.resourceQueryObject(resourceIdentifier)
                           .fetch(resourceService)
                           .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));

        var fileEntry = FileEntry.queryObject(fileIdentifier, resourceIdentifier)
                            .fetch(resourceService)
                            .orElseThrow(() -> new NotFoundException(FILE_NOT_FOUND_MESSAGE));

        validateUpdateFilePermissions(resource, fileEntry, userInstance);

        fileEntry.update(file, userInstance, resourceService);
    }

    private static void validateDeletePermissions(UserInstance userInstance, FileEntry fileEntry, Resource resource)
        throws ForbiddenException {
        if (!new FilePermissions(fileEntry, userInstance, resource)
                 .allowsAction(FileOperation.DELETE)) {
            throw new ForbiddenException();
        }
    }

    private static void validateUploadPermissions(UserInstance userInstance, Resource resource)
        throws ForbiddenException {
        if (!new PublicationPermissions(resource, userInstance)
                 .allowsAction(PublicationOperation.UPLOAD_FILE)) {
            throw new ForbiddenException();
        }
    }

    private static UserUploadDetails createUploadDetails(UserInstance userInstance) {
        return new UserUploadDetails(new Username(userInstance.getUsername()), Instant.now());
    }

    private static void validateUpdateFilePermissions(Resource resource, FileEntry fileEntry, UserInstance userInstance)
        throws ForbiddenException {
        if (!new FilePermissions(fileEntry, userInstance, resource)
                 .allowsAction(FileOperation.WRITE_METADATA)) {
            throw new ForbiddenException();
        }
    }

    private Resource fetchResource(SortableIdentifier resourceIdentifier) throws NotFoundException {
        return Resource.resourceQueryObject(resourceIdentifier)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
    }

    private ObjectMetadata getObjectMetadata(String key) {
        return amazonS3.getObjectMetadata(new GetObjectMetadataRequest(BUCKET_NAME, key));
    }

    private UploadedFile constructUploadedFile(UUID identifier, ObjectMetadata metadata, UserInstance userInstance) {
        return new UploadedFile(identifier, Filename.fromContentDispositionValue(metadata.getContentDisposition()),
                                metadata.getContentType(),
                                metadata.getContentLength(), getRrs(userInstance),
                                createUploadDetails(userInstance));
    }

    private RightsRetentionStrategy getRrs(UserInstance userInstance) {
        return Optional.ofNullable(customerApiClient.fetch(userInstance.getCustomerId()))
                   .map(Customer::getRightsRetentionStrategy)
                   .map(CustomerApiRightsRetention::getType)
                   .map(RightsRetentionStrategyConfiguration::fromValue)
                   .map(this::createRightsRetentionStrategy)
                   .orElse(null);
    }

    private RightsRetentionStrategy createRightsRetentionStrategy(RightsRetentionStrategyConfiguration configuration) {
        if (RIGHTS_RETENTION_STRATEGY.equals(configuration)) {
            return CustomerRightsRetentionStrategy.create(configuration);
        } else {
            return  NullRightsRetentionStrategy.create(configuration);
        }
    }
}
