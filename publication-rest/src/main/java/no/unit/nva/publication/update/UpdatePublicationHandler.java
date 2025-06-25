package no.unit.nva.publication.update;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.FileOperation.WRITE_METADATA;
import static no.unit.nva.model.PublicationOperation.TERMINATE;
import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.defaultEventBridgeClient;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.UnpublishingNote;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
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
import no.unit.nva.publication.permissions.file.FilePermissions;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.rightsretention.RightsRetentionsApplier;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class UpdatePublicationHandler
    extends ApiGatewayHandler<PublicationRequest, PublicationResponse> {

    private static final Logger logger = LoggerFactory.getLogger(UpdatePublicationHandler.class);
    public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE = "Identifiers in path and in body, do not match";
    private static final String ENV_KEY_BACKEND_CLIENT_SECRET_NAME = "BACKEND_CLIENT_SECRET_NAME";
    private static final String ENV_KEY_BACKEND_CLIENT_AUTH_URL = "BACKEND_CLIENT_AUTH_URL";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;
    public static final String NVA_EVENT_BUS_NAME_KEY = "NVA_EVENT_BUS_NAME";
    private static final String API_HOST_ENV_KEY = "API_HOST";
    public static final String LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS =
        "Lambda Function Invocation Result - Success";
    public static final String NVA_PUBLICATION_DELETE_SOURCE = "nva.publication.delete";
    private final EventBridgeClient eventBridgeClient;
    private final String nvaEventBusName;
    private final SecretsReader secretsReader;
    private final HttpClient httpClient;
    private final String apiHost;
    private final JavaHttpClientCustomerApiClient customerApiClient;

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
                                    SecretsManagerClient secretsManagerClient,
                                    HttpClient httpClient) {
        super(PublicationRequest.class, environment);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
        this.eventBridgeClient = eventBridgeClient;
        this.nvaEventBusName = environment.readEnv(NVA_EVENT_BUS_NAME_KEY);
        this.apiHost = environment.readEnv(API_HOST_ENV_KEY);
        this.secretsReader = new SecretsReader(secretsManagerClient);
        this.httpClient = httpClient;
        this.customerApiClient = getCustomerApiClient();
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

        var existingResource = fetchResource(identifierInPath);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var permissionStrategy = PublicationPermissions.create(existingResource, userInstance);
        var updatedPublication = switch (input) {
            case UpdateRequest request -> updatePublication(request,
                                                                  identifierInPath,
                                                                  existingResource,
                                                                  permissionStrategy,
                                                                  userInstance);

            case UnpublishPublicationRequest unpublishPublicationRequest ->
                unpublishPublication(unpublishPublicationRequest,
                                     existingResource.toPublication(),
                                     permissionStrategy,
                                     userInstance);

            case RepublishPublicationRequest ignored -> republish(existingResource, permissionStrategy, userInstance);

            case DeletePublicationRequest ignored -> terminatePublication(existingResource, permissionStrategy, userInstance);

            default -> throw new BadRequestException("Unknown input body type");
        };

        return PublicationResponseFactory.create(updatedPublication, requestInfo, identityServiceClient);
    }

    private Resource updatePublication(UpdateRequest input, SortableIdentifier identifierInPath,
                                       Resource existingResource, PublicationPermissions permissionStrategy,
                                       UserInstance userInstance) throws ApiGatewayException {
        input.authorize(permissionStrategy);
        validateRequest(identifierInPath, input);

        var resourceUpdate = input.generateUpdate(existingResource);
        var customer = fetchCustomerOrFailWithBadGateway(customerApiClient, resourceUpdate.getPublisher().getId());
        authorizeFileEntries(existingResource, userInstance, getUpdatedFiles(existingResource, resourceUpdate));

        var updatedFiles = resourceUpdate.getFiles();
        if (!updatedFiles.stream().allMatch(file -> canUpdateFileToPendingOpenFile(file, customer, existingResource,
                                                                                   resourceUpdate, userInstance))) {
            throw new ForbiddenException();
        }

        setRrsOnFiles(resourceUpdate, existingResource, customer, userInstance.getUsername(), permissionStrategy);

        new PublishingRequestResolver(resourceService, ticketService, userInstance, customer)
            .resolve(existingResource, resourceUpdate);

        return resourceUpdate.update(resourceService, userInstance);
    }

    private Resource fetchResource(SortableIdentifier identifierInPath) throws NotFoundException {
        return Resource.resourceQueryObject(identifierInPath)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
    }

    private Resource republish(Resource resource, PublicationPermissions permissionStrategy,
                                  UserInstance userInstance)
        throws ApiGatewayException {
        return RepublishUtil.create(resourceService, ticketService, permissionStrategy)
                   .republish(resource, userInstance);
    }

    private Resource terminatePublication(Resource resource,
                                             PublicationPermissions permissionStrategy,
                                             UserInstance userInstance)
        throws UnauthorizedException, BadRequestException {
        permissionStrategy.authorize(TERMINATE);

        resourceService.terminateResource(resource, userInstance);

        return Resource.resourceQueryObject(resource.getIdentifier()).fetch(resourceService).orElseThrow();
    }

    private Resource unpublishPublication(UnpublishPublicationRequest unpublishPublicationRequest,
                                             Publication existingPublication,
                                             PublicationPermissions permissionStrategy,
                                             UserInstance userInstance)
        throws ApiGatewayException {
        unpublishPublicationRequest.validate(apiHost);
        permissionStrategy.authorize(UNPUBLISH);

        var updatedPublication = toPublicationWithDuplicate(unpublishPublicationRequest,
                                                            existingPublication,
                                                            userInstance);
        resourceService.unpublishPublication(updatedPublication, userInstance);
        var updatedResource = Resource.resourceQueryObject(updatedPublication.getIdentifier()).fetch(resourceService)
                               .orElseThrow();
        updateNvaDoi(updatedResource);
        return updatedResource;
    }

    private void updateNvaDoi(Resource resource) {
        if (nonNull(resource.getDoi())) {
            logger.info("Publication {} has NVA-DOI, sending event to EventBridge", resource.getIdentifier());
            var putEventsRequest = PutEventsRequest.builder()
                                       .entries(PutEventsRequestEntry.builder()
                                                    .eventBusName(nvaEventBusName)
                                                    .source(NVA_PUBLICATION_DELETE_SOURCE)
                                                    .detailType(LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS)
                                                    .detail(new LambdaDestinationInvocationDetail<>(
                                                        DoiMetadataUpdateEvent.createUpdateDoiEvent(resource.toPublication(),
                                                                                                    apiHost))
                                                                .toJsonString())
                                                    .resources(resource.getIdentifier().toString()).build())
                                       .build();
            var ebResult =
                eventBridgeClient.putEvents(putEventsRequest);

            logger.info("failedEntryCount={}", ebResult.failedEntryCount());
        } else {
            logger.info("Publication {} has no NVA-DOI, no event sent to EventBridge", resource.getIdentifier());
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

    private static boolean canUpdateFileToPendingOpenFile(File file, Customer customer, Resource existingResource,
                                                          Resource updatedResource, UserInstance userInstance) {
        var existingFile = existingResource.getFileByIdentifier(file.getIdentifier());
        return existingFile.isEmpty()
               || !(file instanceof PendingOpenFile)
               || customerAllowsOpenFiles(customer, updatedResource)
               || elevatedUserCanUpdateResource(userInstance);
    }

    private static boolean elevatedUserCanUpdateResource(UserInstance userInstance) {
        return userInstance.getAccessRights().contains(AccessRight.MANAGE_RESOURCES_STANDARD)
                 || userInstance.getAccessRights().contains(AccessRight.MANAGE_RESOURCES_ALL);
    }

    private static boolean customerAllowsOpenFiles(Customer customer, Resource resource) {
        return resource.getInstanceType()
                   .map(instanceType -> customer.getAllowFileUploadForTypes().contains(instanceType))
                   .orElse(false);
    }

    private static void authorizeFileEntries(Resource existingResource, UserInstance userInstance, List<FileEntry> existingFiles)
        throws UnauthorizedException {
        for (var file : existingFiles) {
            new FilePermissions(file, userInstance, existingResource).authorize(WRITE_METADATA);
        }
    }

    private static List<FileEntry> getUpdatedFiles(Resource existingResource,
                                                   Resource updatedResource) {
        return existingResource.getFileEntries().stream()
                   .filter(existingFileEntry -> fileIsPresentAndIsUpdated(updatedResource, existingFileEntry))
                   .toList();
    }

    private static boolean fileIsPresentAndIsUpdated(Resource updatedResource, FileEntry existingFileEntry) {
        var updatedFile = updatedResource.getFileByIdentifier(existingFileEntry.getFile().getIdentifier());
        return updatedFile.filter(file -> isUpdatedFile(existingFileEntry.getFile(), file))
                   .isPresent();
    }

    private static boolean isUpdatedFile(File existingFile, File updatedFile) {
        return !existingFile.equals(updatedFile);
    }

    private void setRrsOnFiles(Resource updatedResource, Resource existingResource, Customer customer,
                               String actingUser, PublicationPermissions permissionStrategy)
        throws BadRequestException, UnauthorizedException {
        RightsRetentionsApplier.rrsApplierForUpdatedPublication(existingResource, updatedResource,
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
        var authorizedBackendClient = AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient,
                                                                                             cognitoCredentials);
        return new JavaHttpClientCustomerApiClient(authorizedBackendClient);
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

    @Override
    protected Integer getSuccessStatusCode(PublicationRequest input, PublicationResponse output) {
        return switch (input) {
            case UpdatePublicationRequest ignored -> HttpStatus.SC_OK;
            case PartialUpdatePublicationRequest ignored -> HttpStatus.SC_OK;
            case UnpublishPublicationRequest ignored -> HttpStatus.SC_ACCEPTED;
            case DeletePublicationRequest ignored -> HttpStatus.SC_ACCEPTED;
            case RepublishPublicationRequest ignored -> HttpStatus.SC_OK;
            default -> HttpStatus.SC_BAD_REQUEST;
        };
    }

    private boolean identifiersDoNotMatch(SortableIdentifier identifierInPath,
                                          UpdateRequest input) {
        return !identifierInPath.equals(input.getIdentifier());
    }

    private void validateRequest(SortableIdentifier identifierInPath, UpdateRequest input)
        throws BadRequestException {
        if (identifiersDoNotMatch(identifierInPath, input)) {
            throw new BadRequestException(IDENTIFIER_MISMATCH_ERROR_MESSAGE);
        }
    }

}