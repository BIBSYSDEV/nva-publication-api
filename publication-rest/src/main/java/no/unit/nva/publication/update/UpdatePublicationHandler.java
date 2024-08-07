package no.unit.nva.publication.update;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationOperation.TERMINATE;
import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static no.unit.nva.publication.RequestUtil.createUserInstanceFromRequest;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.defaultEventBridgeClient;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.validation.PublicationUriValidator.isValid;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.api.PublicationResponseElevatedUser;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.UnpublishingNote;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.File.Builder;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerNotAvailableException;
import no.unit.nva.publication.commons.customer.JavaHttpClientCustomerApiClient;
import no.unit.nva.publication.delete.LambdaDestinationInvocationDetail;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.FileForApproval;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.rightsretention.RightsRetentionsApplier;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.validation.DefaultPublicationValidator;
import no.unit.nva.publication.validation.PublicationValidationException;
import no.unit.nva.publication.validation.PublicationValidator;
import no.unit.nva.s3.S3Driver;
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
    extends ApiGatewayHandler<PublicationRequest, PublicationResponseElevatedUser> {

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
    private final PublicationValidator publicationValidator;
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
        super(PublicationRequest.class, environment);
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

    private static boolean isPending(TicketEntry publishingRequest) {
        return TicketStatus.PENDING.equals(publishingRequest.getStatus());
    }

    @Override
    protected void validateRequest(PublicationRequest publicationRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected PublicationResponseElevatedUser processInput(PublicationRequest input,
                                                           RequestInfo requestInfo,
                                                           Context context)
        throws ApiGatewayException {
        SortableIdentifier identifierInPath = RequestUtil.getIdentifier(requestInfo);

        Publication existingPublication = fetchPublication(identifierInPath);

        var userInstance = RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
        var permissionStrategy = PublicationPermissionStrategy.create(existingPublication, userInstance);
        Publication updatedPublication = switch (input) {
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

            case DeletePublicationRequest ignored -> terminatePublication(existingPublication, permissionStrategy);

            default -> throw new BadRequestException("Unknown input body type");
        };

        var publicationResponse = PublicationResponseElevatedUser.fromPublication(updatedPublication);
        publicationResponse.setAllowedOperations(getAllowedOperations(requestInfo, updatedPublication));

        return publicationResponse;
    }

    private Set<PublicationOperation> getAllowedOperations(RequestInfo requestInfo, Publication publication) {
        return attempt(() -> createUserInstanceFromRequest(requestInfo, identityServiceClient)).toOptional()
                   .map(userInstance -> PublicationPermissionStrategy.create(publication, userInstance))
                   .map(PublicationPermissionStrategy::getAllAllowedActions)
                   .orElse(Collections.emptySet());
    }

    private Publication terminatePublication(Publication existingPublication,
                                             PublicationPermissionStrategy permissionStrategy)
        throws UnauthorizedException, BadRequestException, NotFoundException {
        permissionStrategy.authorize(TERMINATE);

        deleteFiles(existingPublication);
        resourceService.deletePublication(existingPublication);

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
                                             PublicationPermissionStrategy permissionStrategy,
                                             UserInstance userInstance)
        throws ApiGatewayException {
        validateUnpublishRequest(unpublishPublicationRequest);
        permissionStrategy.authorize(UNPUBLISH);

        var updatedPublication = toPublicationWithDuplicate(unpublishPublicationRequest,
                                                            existingPublication,
                                                            userInstance);
        resourceService.unpublishPublication(updatedPublication);
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
                                                        DoiMetadataUpdateEvent.createUpdateDoiEvent(publication))
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
                                       PublicationPermissionStrategy permissionStrategy,
                                       UserInstance userInstance)
        throws ApiGatewayException {
        validateRequest(identifierInPath, input);
        permissionStrategy.authorize(UPDATE);

        validateRemovalOfPublishedFiles(existingPublication, input, permissionStrategy);

        var publicationUpdate = input.generatePublicationUpdate(existingPublication);

        var customerApiClient = getCustomerApiClient();
        var customer = fetchCustomerOrFailWithBadGateway(customerApiClient, publicationUpdate.getPublisher().getId());
        validatePublication(publicationUpdate, customer);
        updateAssociatedArtifactList(existingPublication.getAssociatedArtifacts(),
                                     publicationUpdate.getAssociatedArtifacts(),
                                     extractUploadDetails(userInstance));
        setRrsOnFiles(publicationUpdate, existingPublication, customer, userInstance.getUsername(), permissionStrategy);
        upsertPublishingRequestIfNeeded(existingPublication, publicationUpdate, customer, userInstance);

        return resourceService.updatePublication(publicationUpdate);
    }

    private static UploadDetails extractUploadDetails(UserInstance userInstance) {
        return new UploadDetails(new Username(userInstance.getUsername()), Instant.now());
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

    private static void updateAssociatedArtifacts(AssociatedArtifactList updatedArtifacts,
                                                  UploadDetails uploadDetails,
                                                  File item) throws BadRequestException {
        var index = updatedArtifacts.indexOf(item);
        var updated = updateFileWithUploadDetails(item, uploadDetails);
        updatedArtifacts.set(index, updated);
    }

    private static File updateFileWithUploadDetails(File file, UploadDetails uploadDetails) throws BadRequestException {
        return switch (file) {
            case PublishedFile publishedFile -> addUploadDetails(publishedFile, uploadDetails).buildPublishedFile();
            case UnpublishedFile unpublishedFile -> addUploadDetails(unpublishedFile, uploadDetails)
                                                        .buildUnpublishedFile();
            case AdministrativeAgreement unpublishableFile -> addUploadDetails(unpublishableFile, uploadDetails)
                                                                  .buildUnpublishableFile();
            default -> throw new BadRequestException("Unsupported file type: " + file);
        };
    }

    private static Builder addUploadDetails(File file, UploadDetails uploadDetails) {
        return file.copy().withUploadDetails(uploadDetails);
    }

    private void validateRemovalOfPublishedFiles(Publication existingPublication,
                                                 UpdatePublicationRequest input,
                                                 PublicationPermissionStrategy permissionStrategy)
        throws ForbiddenException {
        var inputFiles = input.getAssociatedArtifacts().stream()
                             .filter(PublishedFile.class::isInstance)
                             .map(PublishedFile.class::cast).toList();
        var existingFiles = existingPublication.getAssociatedArtifacts().stream()
                                .filter(PublishedFile.class::isInstance)
                                .map(PublishedFile.class::cast);

        if (!filesAreUnchangedExceptLicense(existingFiles, inputFiles) && !permissionStrategy.isCuratorOnPublication()) {
            throw new ForbiddenException();
        }
    }

    private static boolean filesAreUnchangedExceptLicense(Stream<PublishedFile> existingFiles, List<PublishedFile> inputFiles) {
        return existingFiles.allMatch(existingFile -> inputFiles.stream().anyMatch(inputFile -> inputFile.equalsExceptLicense(existingFile)));
    }

    private void setRrsOnFiles(Publication publicationUpdate, Publication existingPublication, Customer customer,
                               String actingUser, PublicationPermissionStrategy permissionStrategy)
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


    private void upsertPublishingRequestIfNeeded(Publication existingPublication,
                                                 Publication publicationUpdate,
                                                 Customer customer,
                                                 UserInstance userInstance) {
        if (isAlreadyPublished(existingPublication) && !thereIsRelatedPendingPublishingRequest(publicationUpdate)) {
            createPublishingRequestOnFileUpdate(publicationUpdate, customer, userInstance);
        }
        if (isAlreadyPublished(existingPublication) && thereAreNoFiles(publicationUpdate)) {
            autoCompletePendingPublishingRequestsIfNeeded(publicationUpdate, userInstance);
        }
        if (isAlreadyPublished(existingPublication) && updateHasFileChanges(existingPublication, publicationUpdate)) {
            updateFilesForApproval(publicationUpdate);
        }
    }

    private void updateFilesForApproval(Publication publicationUpdate) {
        var filesForApproval = getUnpublishedFiles(publicationUpdate).stream()
                                   .map(FileForApproval::fromFile)
                                   .collect(Collectors.toSet());
        fetchPendingPublishingRequest(publicationUpdate)
            .forEach(publishingRequestCase -> updateFilesForApproval(publishingRequestCase, filesForApproval));
    }

    private void updateFilesForApproval(PublishingRequestCase publishingRequestCase,
                                        Set<FileForApproval> filesForApproval) {
        publishingRequestCase.setFilesForApproval(filesForApproval);
        publishingRequestCase.persistUpdate(ticketService);
    }

    private boolean updateHasFileChanges(Publication existingPublication, Publication publicationUpdate) {
        var existingFiles = getUnpublishedFiles(existingPublication);
        var updatedFiles = getUnpublishedFiles(publicationUpdate);
        return !(new HashSet<>(existingFiles).containsAll(updatedFiles)
               && new HashSet<>(updatedFiles).containsAll(existingFiles));
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

    private void validatePublication(Publication publicationUpdate, Customer customer) throws BadRequestException {
        try {
            publicationValidator.validate(publicationUpdate, customer);
        } catch (PublicationValidationException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private void autoCompletePendingPublishingRequestsIfNeeded(Publication publication, UserInstance userInstance) {
        fetchPendingPublishingRequest(publication)
            .map(PublishingRequestCase.class::cast)
            .forEach(ticket -> ticket.complete(publication, new Username(userInstance.getUsername()))
                                   .persistUpdate(ticketService));
    }

    private Stream<PublishingRequestCase> fetchPendingPublishingRequest(Publication publication) {
        return ticketService.fetchTicketsForUser(UserInstance.fromPublication(publication))
                   .filter(PublishingRequestCase.class::isInstance)
                   .filter(UpdatePublicationHandler::isPending)
                   .map(PublishingRequestCase.class::cast);
    }

    private boolean thereAreNoFiles(Publication publicationUpdate) {
        return publicationUpdate.getAssociatedArtifacts().stream()
                   .noneMatch(File.class::isInstance);
    }

    @Override
    protected Integer getSuccessStatusCode(PublicationRequest input, PublicationResponseElevatedUser output) {
        return switch (input) {
            case UpdatePublicationRequest ignored -> HttpStatus.SC_OK;
            case UnpublishPublicationRequest ignored -> HttpStatus.SC_ACCEPTED;
            case DeletePublicationRequest ignored -> HttpStatus.SC_ACCEPTED;
            default -> HttpStatus.SC_BAD_REQUEST;
        };
    }

    private boolean containsNewPublishableFiles(Publication publicationUpdate) {
        var unpublishedFiles = getUnpublishedFiles(publicationUpdate);
        return !unpublishedFiles.isEmpty() && containsPublishableFile(unpublishedFiles);
    }

    private boolean containsPublishableFile(List<File> unpublishedFiles) {
        return unpublishedFiles.stream().anyMatch(this::isPublishable);
    }

    private boolean hasMatchingIdentifier(Publication publication, TicketEntry ticketEntry) {
        return ticketEntry.getResourceIdentifier().equals(publication.getIdentifier());
    }

    private boolean identifiersDoNotMatch(SortableIdentifier identifierInPath,
                                          UpdatePublicationRequest input) {
        return !identifierInPath.equals(input.getIdentifier());
    }

    private boolean isAlreadyPublished(Publication existingPublication) {
        return PublicationStatus.PUBLISHED.equals(existingPublication.getStatus())
               || PublicationStatus.PUBLISHED_METADATA.equals(existingPublication.getStatus());
    }

    private boolean isPublishable(AssociatedArtifact artifact) {
        var file = (File) artifact;
        return nonNull(file.getLicense()) && !file.isAdministrativeAgreement();
    }

    private boolean thereIsRelatedPendingPublishingRequest(Publication publication) {
        return ticketService.fetchTicketsForUser(UserInstance.fromPublication(publication))
                   .filter(PublishingRequestCase.class::isInstance)
                   .filter(ticketEntry -> hasMatchingIdentifier(publication, ticketEntry))
                   .anyMatch(UpdatePublicationHandler::isPending);
    }

    private List<File> getUnpublishedFiles(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(UnpublishedFile.class::isInstance)
                   .map(UnpublishedFile.class::cast)
                   .collect(Collectors.toList());
    }

    private Publication fetchPublication(SortableIdentifier identifierInPath) throws NotFoundException {
        return attempt(() -> resourceService.getPublicationByIdentifier(identifierInPath))
                   .orElseThrow(failure -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
    }

    private void createPublishingRequestOnFileUpdate(Publication publicationUpdate, Customer customer,
                                                     UserInstance userInstance) {
        if (containsNewPublishableFiles(publicationUpdate)) {
            persistPendingPublishingRequest(publicationUpdate, customer, userInstance);
        }
    }

    private void persistPendingPublishingRequest(Publication publicationUpdate,
                                                 Customer customer,
                                                 UserInstance userInstance) {
        attempt(() -> TicketEntry.requestNewTicket(publicationUpdate, PublishingRequestCase.class))
            .map(publishingRequest -> injectPublishingWorkflow((PublishingRequestCase) publishingRequest, customer))
            .map(publishingRequest ->
                     persistPublishingRequest(publicationUpdate, userInstance, customer, publishingRequest));
    }

    private TicketEntry persistPublishingRequest(Publication publicationUpdate, UserInstance userInstance,
                                                 Customer customer, PublishingRequestCase publishingRequest)
        throws ApiGatewayException {
        return userInstance.getAccessRights().contains(MANAGE_PUBLISHING_REQUESTS)
               || useIsAllowedToPublishFiles(customer)
                   ? publishingRequest.persistAutoComplete(ticketService, publicationUpdate,
                                                           new Username(userInstance.getUsername()))
                   : publishingRequest.persistNewTicket(ticketService);
    }

    private static boolean useIsAllowedToPublishFiles(Customer customer) {
        return PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES.getValue()
                   .equals(customer.getPublicationWorkflow());
    }

    private PublishingRequestCase injectPublishingWorkflow(PublishingRequestCase ticket, Customer customer) {
        ticket.setWorkflow(PublishingWorkflow.lookUp(customer.getPublicationWorkflow()));
        return ticket;
    }

    private void validateRequest(SortableIdentifier identifierInPath, UpdatePublicationRequest input)
        throws BadRequestException {
        if (identifiersDoNotMatch(identifierInPath, input)) {
            throw new BadRequestException(IDENTIFIER_MISMATCH_ERROR_MESSAGE);
        }
    }
}