package no.unit.nva.publication.file.upload;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

public class FileService {

    private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found!";
    private static final String FILE_NOT_FOUND_MESSAGE = "File not found!";
    private final S3Client s3Client;
    private final CustomerApiClient customerApiClient;
    private final ResourceService resourceService;

    public FileService(S3Client s3Client, CustomerApiClient customerApiClient, ResourceService resourceService) {
        this.s3Client = s3Client;
        this.customerApiClient = customerApiClient;
        this.resourceService = resourceService;
    }

    @JacocoGenerated
    public static FileService defaultFileService() {
        return new FileService(S3Client.create(), JavaHttpClientCustomerApiClient.defaultInstance(),
                               ResourceService.defaultService());
    }

    public CreateMultipartUploadResponse initiateMultipartUpload(SortableIdentifier resourceIdentifier,
                                                                 UserInstance userInstance,
                                                                 CreateUploadRequestBody createUploadRequestBody)
        throws NotFoundException, ForbiddenException {

        var resource = fetchResource(resourceIdentifier);

        validateUploadPermissions(userInstance, resource);

        var request = createUploadRequestBody.toCreateMultipartUploadRequest(BUCKET_NAME);

        return s3Client.createMultipartUpload(request);
    }

    public File completeMultipartUpload(SortableIdentifier resourceIdentifier,
                                        CompleteUploadRequest request, UserInstance userInstance)
        throws NotFoundException, BadRequestException, ForbiddenException {

        var resource = fetchResource(resourceIdentifier);

        var completeMultipartUploadRequest = request.toCompleteMultipartUploadRequest(BUCKET_NAME);
        var completeMultipartUploadResponse = s3Client.completeMultipartUpload(completeMultipartUploadRequest);
        var s3ObjectKey = completeMultipartUploadResponse.key();
        var headObjectResponse = getObjectMetadata(s3ObjectKey);

        var file = userInstance.isExternalClient() && request instanceof ExternalCompleteUploadRequest externalRequest
                       ? constructFileForExternalClient(UUID.fromString(s3ObjectKey), externalRequest, headObjectResponse)
                       : constructUploadedFile(UUID.fromString(s3ObjectKey), headObjectResponse, userInstance);

        validateUploadPermissions(userInstance, resource);

        FileEntry.create(file, resource.getIdentifier(), userInstance).persist(resourceService, userInstance);
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
                                               HeadObjectResponse metadata) throws BadRequestException {
        var builder = File.builder()
                          .withIdentifier(identifier)
                          .withName(Filename.fromContentDispositionValue(metadata.contentDisposition()))
                          .withSize(metadata.contentLength())
                          .withMimeType(metadata.contentType())
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

    private HeadObjectResponse getObjectMetadata(String key) {
        return s3Client.headObject(HeadObjectRequest.builder().bucket(BUCKET_NAME).key(key).build());
    }

    private UploadedFile constructUploadedFile(UUID identifier, HeadObjectResponse metadata, UserInstance userInstance) {
        return new UploadedFile(identifier, Filename.fromContentDispositionValue(metadata.contentDisposition()),
                                metadata.contentType(),
                                metadata.contentLength(), getRrs(userInstance),
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
