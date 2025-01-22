package no.unit.nva.publication.update;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationOperation.TERMINATE;
import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.model.PublicationOperation.UPDATE_FILES;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.defaultEventBridgeClient;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.validation.PublicationUriValidator.isValid;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.UnpublishingNote;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.File.Builder;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingInternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.publication.PublicationResponseFactory;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerNotAvailableException;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.delete.LambdaDestinationInvocationDetail;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.rightsretention.RightsRetentionsApplier;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.validation.DefaultPublicationValidator;
import no.unit.nva.publication.validation.PublicationValidationException;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@SuppressWarnings("PMD.GodClass")
public class UpdatePublicationHandler
    extends ApiGatewayHandler<PublicationRequest, PublicationResponse> {

    private static final Logger logger = LoggerFactory.getLogger(UpdatePublicationHandler.class);
    public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE = "Identifiers in path and in body, do not match";
    private static final String ENV_KEY_BACKEND_CLIENT_SECRET_NAME = "BACKEND_CLIENT_SECRET_NAME";
    private static final String ENV_KEY_BACKEND_CLIENT_AUTH_URL = "BACKEND_CLIENT_AUTH_URL";
    public static final String UNPUBLISH_REQUEST_REQUIRES_A_COMMENT = "Unpublish request requires a comment";
    public static final String DUPLICATE_OF_MUST_BE_A_PUBLICATION_API_URI =
        "The duplicateOf field must be a valid publication API URI";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;
    public static final String NVA_EVENT_BUS_NAME_KEY = "NVA_EVENT_BUS_NAME";
    private static final String API_HOST_ENV_KEY = "API_HOST";
    public static final String LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS =
        "Lambda Function Invocation Result - Success";
    public static final String NVA_PUBLICATION_DELETE_SOURCE = "nva.publication.delete";
    public static final String NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY = "NVA_PERSISTED_STORAGE_BUCKET_NAME";
    private final EventBridgeClient eventBridgeClient;
    private final String nvaEventBusName;
    private final S3Driver s3Driver;
    private final SecretsReader secretsReader;
    private final HttpClient httpClient;
    private final DefaultPublicationValidator publicationValidator;
    private final String apiHost;

    /**
     * Default constructor for MainHandler.
     */
    @JacocoGenerated
    public UpdatePublicationHandler() {
        this(ResourceService.defaultService(),
             TicketService.defaultService(),
             new Environment(),
             IdentityServiceClient.prepare(),
             defaultEventBridgeClient(),
             S3Driver.defaultS3Client().build(),
             SecretsReader.defaultSecretsManagerClient(),
             HttpClient.newHttpClient());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param resourceService       publicationService
     * @param environment           environment
     */
    public UpdatePublicationHandler(ResourceService resourceService,
                                    TicketService ticketService,
                                    Environment environment,
                                    IdentityServiceClient identityServiceClient,
                                    EventBridgeClient eventBridgeClient,
                                    S3Client s3Client,
                                    SecretsManagerClient secretsManagerClient,
                                    HttpClient httpClient) {
        super(PublicationRequest.class, environment, httpClient);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
        this.eventBridgeClient = eventBridgeClient;
        this.nvaEventBusName = environment.readEnv(NVA_EVENT_BUS_NAME_KEY);
        this.apiHost = environment.readEnv(API_HOST_ENV_KEY);
        this.s3Driver = new S3Driver(s3Client, environment.readEnv(NVA_PERSISTED_STORAGE_BUCKET_NAME_KEY));
        this.secretsReader = new SecretsReader(secretsManagerClient);
        this.publicationValidator = new DefaultPublicationValidator();
        this.httpClient = httpClient;
    }

    @Override
    protected void validateRequest(PublicationRequest publicationRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected PublicationResponse processInput(PublicationRequest input,
                                               RequestInfo requestInfo,
                                               Context context)
        throws ApiGatewayException {
        var identifierInPath = RequestUtil.getIdentifier(requestInfo);

        var existingPublication = fetchPublication(identifierInPath);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var permissionStrategy = PublicationPermissions.create(existingPublication, userInstance);
        var updatedPublication = switch (input) {
            case UpdatePublicationRequest publicationMetadata -> updateMetadata(publicationMetadata,
                                                                                identifierInPath,
                                                                                existingPublication,
                                                                                permissionStrategy,
                                                                                userInstance);

            case UnpublishPublicationRequest unpublishPublicationRequest ->
                unpublishPublication(unpublishPublicationRequest,
                                     existingPublication,
                                     permissionStrategy,
                                     userInstance);

            case RepublishPublicationRequest ignored -> republish(existingPublication, permissionStrategy, userInstance);

            case DeletePublicationRequest ignored -> terminatePublication(existingPublication, permissionStrategy, userInstance);

            default -> throw new BadRequestException("Unknown input body type");
        };

        return PublicationResponseFactory.create(updatedPublication, requestInfo, identityServiceClient);
    }

    private Publication fetchPublication(SortableIdentifier identifierInPath) throws NotFoundException {
        return Resource.resourceQueryObject(identifierInPath)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE))
                   .toPublication();
    }

    private Publication republish(Publication existingPublication, PublicationPermissions permissionStrategy,
                                  UserInstance userInstance)
        throws ApiGatewayException {
        return RepublishUtil.create(resourceService, ticketService, permissionStrategy)
                   .republish(existingPublication, userInstance);
    }

    private Publication terminatePublication(Publication existingPublication,
                                             PublicationPermissions permissionStrategy,
                                             UserInstance userInstance)
        throws UnauthorizedException, BadRequestException, NotFoundException {
        permissionStrategy.authorize(TERMINATE);

        deleteFiles(existingPublication);
        resourceService.deletePublication(existingPublication, userInstance);

        return resourceService.getPublication(existingPublication);
    }

    private void deleteFiles(Publication publication) {
        publication.getAssociatedArtifacts()
            .stream()
            .filter(File.class::isInstance)
            .map(File.class::cast)
            .map(File::getIdentifier)
            .map(UUID::toString)
            .map(UnixPath::of)
            .forEach(s3Driver::deleteFile);
    }

    private Publication unpublishPublication(UnpublishPublicationRequest unpublishPublicationRequest,
                                             Publication existingPublication,
                                             PublicationPermissions permissionStrategy,
                                             UserInstance userInstance)
        throws ApiGatewayException {
        validateUnpublishRequest(unpublishPublicationRequest);
        permissionStrategy.authorize(UNPUBLISH);

        var updatedPublication = toPublicationWithDuplicate(unpublishPublicationRequest,
                                                            existingPublication,
                                                            userInstance);
        resourceService.unpublishPublication(updatedPublication, userInstance);
        updatedPublication = resourceService.getPublication(updatedPublication);
        updateNvaDoi(updatedPublication);
        return updatedPublication;
    }

    private void validateUnpublishRequest(UnpublishPublicationRequest unpublishPublicationRequest)
        throws BadRequestException {
        if (isNull(unpublishPublicationRequest.getComment())) {
            throw new BadRequestException(UNPUBLISH_REQUEST_REQUIRES_A_COMMENT);
        }

        var duplicateUri = unpublishPublicationRequest.getDuplicateOf();

        if (duplicateUri.isPresent()) {
            duplicateUri.filter(uri -> isValid(uri, apiHost))
                 .orElseThrow(() -> new BadRequestException(DUPLICATE_OF_MUST_BE_A_PUBLICATION_API_URI));
        }
    }

    private void updateNvaDoi(Publication publication) {
        if (nonNull(publication.getDoi())) {
            logger.info("Publication {} has NVA-DOI, sending event to EventBridge", publication.getIdentifier());
            var putEventsRequest = PutEventsRequest.builder()
                                       .entries(PutEventsRequestEntry.builder()
                                                    .eventBusName(nvaEventBusName)
                                                    .source(NVA_PUBLICATION_DELETE_SOURCE)
                                                    .detailType(LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS)
                                                    .detail(new LambdaDestinationInvocationDetail<>(
                                                        DoiMetadataUpdateEvent.createUpdateDoiEvent(publication, apiHost))
                                                                .toJsonString())
                                                    .resources(publication.getIdentifier().toString()).build())
                                       .build();
            var ebResult =
                eventBridgeClient.putEvents(putEventsRequest);

            logger.info("failedEntryCount={}", ebResult.failedEntryCount());
        } else {
            logger.info("Publication {} has no NVA-DOI, no event sent to EventBridge", publication.getIdentifier());
        }
    }



    private Publication toPublicationWithDuplicate(UnpublishPublicationRequest unpublishPublicationRequest,
                                                   Publication publication, UserInstance userInstance) {
        var duplicate = unpublishPublicationRequest.getDuplicateOf().orElse(null);
        var comment = unpublishPublicationRequest.getComment();

        var notes = new ArrayList<>(publication.getPublicationNotes());
        notes.add(new UnpublishingNote(comment, new Username(userInstance.getUsername()), Instant.now()));

        return publication.copy()
                   .withDuplicateOf(duplicate)
                   .withPublicationNotes(notes)
                   .build();
    }

    private Publication updateMetadata(UpdatePublicationRequest input,
                                       SortableIdentifier identifierInPath,
                                       Publication existingPublication,
                                       PublicationPermissions permissionStrategy,
                                       UserInstance userInstance)
        throws ApiGatewayException {
        validateRequest(identifierInPath, input);

        if (openFilesAreUnchanged(existingPublication, input)) {
            permissionStrategy.authorize(UPDATE);
        } else {
            permissionStrategy.authorize(UPDATE_FILES);
        }

        var publicationUpdate = input.generatePublicationUpdate(existingPublication);

        var customerApiClient = getCustomerApiClient();
        var customer = fetchCustomerOrFailWithBadGateway(customerApiClient, publicationUpdate.getPublisher().getId());
        validatePublication(publicationUpdate, existingPublication, customer);
        updateAssociatedArtifactList(existingPublication.getAssociatedArtifacts(),
                                     publicationUpdate.getAssociatedArtifacts(),
                                     extractUploadDetails(userInstance));
        setRrsOnFiles(publicationUpdate, existingPublication, customer, userInstance.getUsername(), permissionStrategy);
        new PublishingRequestResolver(resourceService, ticketService, userInstance, customer)
            .resolve(existingPublication, publicationUpdate);

        if (resourceService.shouldUseNewFiles()) {
            Resource.fromPublication(publicationUpdate).getAssociatedArtifacts().stream()
                .filter(File.class::isInstance)
                .map(File.class::cast)
                .forEach(file -> updateFile(existingPublication, file));
            publicationUpdate.getAssociatedArtifacts().removeIf(File.class::isInstance);
        }

        return resourceService.updatePublication(publicationUpdate);
    }

    private void updateFile(Publication existingPublication, File file) {
        FileEntry.queryObject(file.getIdentifier(), existingPublication.getIdentifier())
            .fetch(resourceService)
            .ifPresent(fileEntry -> fileEntry.update(file, resourceService));
    }

    private static UserUploadDetails extractUploadDetails(UserInstance userInstance) {
        return new UserUploadDetails(new Username(userInstance.getUsername()), Instant.now());
    }

    private static void updateAssociatedArtifactList(AssociatedArtifactList originalArtifacts,
                                                     AssociatedArtifactList updatedArtifacts,
                                                     UploadDetails uploadDetails) throws BadRequestException {
        if (originalArtifacts.equals(updatedArtifacts)) {
            return;
        }

        var originalFileIdentifiers = originalArtifacts.stream()
                                              .filter(File.class::isInstance)
                                              .map(f -> ((File) f).getIdentifier())
                                              .toList();

        for (var updatedArtifact : updatedArtifacts) {
            if (updatedArtifact instanceof File file && !originalFileIdentifiers.contains(file.getIdentifier())) {
                updateAssociatedArtifacts(updatedArtifacts, uploadDetails, file);
            }
        }
    }

    //TODO: Should this method also compare changes of internal files?
    private static boolean openFilesAreUnchanged(Publication existingPublication,
                                                 UpdatePublicationRequest input) {
        var inputFiles = input.getAssociatedArtifacts().stream()
                             .filter(OpenFile.class::isInstance)
                             .map(OpenFile.class::cast).toList();
        var existingFiles = existingPublication.getAssociatedArtifacts().stream()
                                .filter(OpenFile.class::isInstance)
                                .map(OpenFile.class::cast);
        return existingFiles.allMatch(inputFiles::contains);
    }

    private static void updateAssociatedArtifacts(AssociatedArtifactList updatedArtifacts,
                                                  UploadDetails uploadDetails,
                                                  File item) throws BadRequestException {
        var index = updatedArtifacts.indexOf(item);
        var updated = updateFileWithUploadDetails(item, uploadDetails);
        updatedArtifacts.set(index, updated);
    }

    private static File updateFileWithUploadDetails(File file, UploadDetails uploadDetails) throws BadRequestException {
        return switch (file) {
            case PendingOpenFile pendingOpenFile -> addUploadDetails(pendingOpenFile, uploadDetails).buildPendingOpenFile();
            case OpenFile openFile -> addUploadDetails(openFile, uploadDetails).buildOpenFile();
            case PendingInternalFile pendingInternalFile -> addUploadDetails(pendingInternalFile, uploadDetails).buildPendingInternalFile();
            case InternalFile internalFile -> addUploadDetails(internalFile, uploadDetails).buildInternalFile();
            case HiddenFile hiddenFile -> addUploadDetails(hiddenFile, uploadDetails).buildHiddenFile();
            default -> throw new BadRequestException("Unsupported file type: " + file);
        };
    }

    private static Builder addUploadDetails(File file, UploadDetails uploadDetails) {
        return file.copy().withUploadDetails(uploadDetails);
    }

    private void setRrsOnFiles(Publication publicationUpdate, Publication existingPublication, Customer customer,
                               String actingUser, PublicationPermissions permissionStrategy)
        throws BadRequestException, UnauthorizedException {
        RightsRetentionsApplier.rrsApplierForUpdatedPublication(existingPublication, publicationUpdate,
                                                                customer.getRightsRetentionStrategy(), actingUser,
                                                                permissionStrategy).handle();


    }

    private JavaHttpClientCustomerApiClient getCustomerApiClient() {
        var backendClientSecretName = environment.readEnv(ENV_KEY_BACKEND_CLIENT_SECRET_NAME);
        var backendClientCredentials = secretsReader.fetchClassSecret(backendClientSecretName,
                                                                      BackendClientCredentials.class);
        var cognitoServerUri = URI.create(environment.readEnv(ENV_KEY_BACKEND_CLIENT_AUTH_URL));
        var cognitoCredentials = new CognitoCredentials(backendClientCredentials::getId,
                                                        backendClientCredentials::getSecret,
                                                        cognitoServerUri);
        return new JavaHttpClientCustomerApiClient(httpClient, cognitoCredentials);
    }

    private static Customer fetchCustomerOrFailWithBadGateway(CustomerApiClient customerApiClient,
                                                              URI customerUri) throws BadGatewayException {
        try {
            return customerApiClient.fetch(customerUri);
        } catch (CustomerNotAvailableException e) {
            logger.error("Problems fetching customer", e);
            throw new BadGatewayException("Customer API not responding or not responding as expected!");
        }
    }

    private void validatePublication(Publication publicationUpdate, Publication existingPublication, Customer customer)
        throws BadRequestException {
        try {
            publicationValidator.validateUpdate(publicationUpdate, existingPublication, customer);
        } catch (PublicationValidationException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    protected Integer getSuccessStatusCode(PublicationRequest input, PublicationResponse output) {
        return switch (input) {
            case UpdatePublicationRequest ignored -> HttpStatus.SC_OK;
            case UnpublishPublicationRequest ignored -> HttpStatus.SC_ACCEPTED;
            case DeletePublicationRequest ignored -> HttpStatus.SC_ACCEPTED;
            case RepublishPublicationRequest ignored -> HttpStatus.SC_OK;
            default -> HttpStatus.SC_BAD_REQUEST;
        };
    }

    private boolean identifiersDoNotMatch(SortableIdentifier identifierInPath,
                                          UpdatePublicationRequest input) {
        return !identifierInPath.equals(input.getIdentifier());
    }

    private void validateRequest(SortableIdentifier identifierInPath, UpdatePublicationRequest input)
        throws BadRequestException {
        if (identifiersDoNotMatch(identifierInPath, input)) {
            throw new BadRequestException(IDENTIFIER_MISMATCH_ERROR_MESSAGE);
        }
    }
}