package no.unit.nva.publication.update;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.FileOperation.WRITE_METADATA;
import static no.unit.nva.model.PublicationOperation.TERMINATE;
import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.defaultEventBridgeClient;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static org.apache.hc.core5.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.apache.http.HttpHeaders.ETAG;

import com.amazonaws.services.lambda.runtime.Context;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.clients.IdentityServiceUnavailableException;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.UnpublishingNote;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.FileStatus;
import no.unit.nva.publication.PublicationResponseFactory;
import no.unit.nva.publication.RequestUtil;
import no.unit.nva.publication.delete.LambdaDestinationInvocationDetail;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;
import no.unit.nva.publication.model.FileWithoutLicenseException;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissions;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.rightsretention.FileRightsRetentionService;
import no.unit.nva.publication.service.impl.RepublishingService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.validation.ETag;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.PreconditionFailedException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class UpdatePublicationHandler
    extends ApiGatewayHandler<PublicationRequest, PublicationResponse> {

  private static final Logger logger = LoggerFactory.getLogger(UpdatePublicationHandler.class);
  public static final String IDENTIFIER_MISMATCH_ERROR_MESSAGE =
      "Identifiers in path and in body, do not match";
  public static final String ETAG_DOES_NOT_MATCH_MESSAGE =
      "The provided ETag does not match the current state of the resource.";
  public static final String ILLEGAL_FILE_TRANSITION_MESSAGE = "%s cannot be updated to %s";
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
  private final String apiHost;

  /** Default constructor for MainHandler. */
  @JacocoGenerated
  public UpdatePublicationHandler() {
    this(
        ResourceService.defaultService(),
        TicketService.defaultService(),
        new Environment(),
        IdentityServiceClient.prepare(),
        defaultEventBridgeClient());
  }

  /**
   * Constructor for MainHandler.
   *
   * @param resourceService publicationService
   * @param environment environment
   */
  public UpdatePublicationHandler(
      ResourceService resourceService,
      TicketService ticketService,
      Environment environment,
      IdentityServiceClient identityServiceClient,
      EventBridgeClient eventBridgeClient) {
    super(PublicationRequest.class, environment);
    this.resourceService = resourceService;
    this.ticketService = ticketService;
    this.identityServiceClient = identityServiceClient;
    this.eventBridgeClient = eventBridgeClient;
    this.nvaEventBusName = environment.readEnv(NVA_EVENT_BUS_NAME_KEY);
    this.apiHost = environment.readEnv(API_HOST_ENV_KEY);
  }

  @Override
  protected void validateRequest(
      PublicationRequest publicationRequest, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    // Do nothing
  }

  @Override
  protected PublicationResponse processInput(
      PublicationRequest input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var identifierInPath = RequestUtil.getIdentifier(requestInfo);
    var clientETag = RequestUtil.getETagValueFromIfMatchHeader(requestInfo).map(ETag::fromString);

    var existingResource = fetchResource(identifierInPath);

    if (clientETag.isPresent()) {
      logger.info("ETag provided {} for resource {}", clientETag.get(), identifierInPath);
      var serverETag =
          ETag.create(requestInfo.getUserNameOptional().orElse(null), getVersion(existingResource));
      if (!serverETag.equals(clientETag.get())) {
        throw new PreconditionFailedException(ETAG_DOES_NOT_MATCH_MESSAGE);
      }
    } else {
      logger.info("No ETag provided for resource {}", identifierInPath);
    }

    var userInstance =
        RequestUtil.createUserInstanceFromRequest(requestInfo, identityServiceClient);
    var updatedResource = applyRequest(input, identifierInPath, existingResource, userInstance);
    addEtagHeaderForUpdatedPublication(
        requestInfo.getUserNameOptional().orElse(null), updatedResource);
    return PublicationResponseFactory.create(updatedResource, requestInfo, identityServiceClient);
  }

  private Resource applyRequest(
      PublicationRequest input,
      SortableIdentifier identifierInPath,
      Resource existingResource,
      UserInstance userInstance)
      throws ApiGatewayException {
    var permissionStrategy = PublicationPermissions.create(existingResource, userInstance);
    try {
      return switch (input) {
        case UpdateRequest request ->
            updatePublication(
                request, identifierInPath, existingResource, permissionStrategy, userInstance);

        case UnpublishPublicationRequest unpublishPublicationRequest ->
            unpublishPublication(
                unpublishPublicationRequest,
                existingResource.toPublication(),
                permissionStrategy,
                userInstance);

        case RepublishPublicationRequest ignored -> republish(existingResource, userInstance);

        case DeletePublicationRequest ignored ->
            terminatePublication(existingResource, permissionStrategy, userInstance);

        default -> throw new BadRequestException("Unknown input body type");
      };
    } catch (IdentityServiceUnavailableException e) {
      throw identityServiceUnavailable(e);
    }
  }

  private static String getVersion(Resource existingResource) {
    return existingResource.getVersion().toString();
  }

  private void addEtagHeaderForUpdatedPublication(String userName, Resource updatedResource) {
    addAdditionalHeaders(
        () ->
            Map.of(
                ETAG,
                ETag.create(userName, getVersion(updatedResource)).toString(),
                ACCESS_CONTROL_EXPOSE_HEADERS,
                ETAG));
  }

  private Resource updatePublication(
      UpdateRequest input,
      SortableIdentifier identifierInPath,
      Resource existingResource,
      PublicationPermissions permissionStrategy,
      UserInstance userInstance)
      throws ApiGatewayException {
    input.authorize(permissionStrategy, existingResource);
    validateRequest(identifierInPath, input);

    var resourceUpdate = input.generateUpdate(existingResource);
    var customer = identityServiceClient.getCustomerById(userInstance.getCustomerId());
    authorizeFileEntries(
        existingResource, userInstance, getUpdatedFiles(existingResource, resourceUpdate));
    validateFileTransitions(existingResource, resourceUpdate);

    var updatedFiles = resourceUpdate.getFiles();
    if (!updatedFiles.stream()
        .allMatch(
            file ->
                canUpdateFileToPendingOpenFile(
                    file, customer, existingResource, resourceUpdate, userInstance))) {
      throw new ForbiddenException();
    }

    setRrsOnFiles(resourceUpdate, existingResource, customer, userInstance);

    try {
      new PublishingRequestResolver(resourceService, ticketService, userInstance, customer)
          .resolve(existingResource, resourceUpdate);
    } catch (FileWithoutLicenseException e) {
      throw fileWithoutLicense(e);
    }

    return resourceUpdate.update(resourceService, userInstance);
  }

  private Resource fetchResource(SortableIdentifier identifierInPath) throws NotFoundException {
    return Resource.resourceQueryObject(identifierInPath)
        .fetch(resourceService)
        .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
  }

  private Resource republish(Resource resource, UserInstance userInstance)
      throws ApiGatewayException {
    try {
      return new RepublishingService(resourceService, identityServiceClient)
          .republish(resource, userInstance);
    } catch (FileWithoutLicenseException e) {
      throw fileWithoutLicense(e);
    }
  }

  private Resource terminatePublication(
      Resource resource, PublicationPermissions permissionStrategy, UserInstance userInstance)
      throws UnauthorizedException, BadRequestException {
    permissionStrategy.authorize(TERMINATE);

    resourceService.terminateResource(resource, userInstance);

    return Resource.resourceQueryObject(resource.getIdentifier())
        .fetch(resourceService)
        .orElseThrow();
  }

  private Resource unpublishPublication(
      UnpublishPublicationRequest unpublishPublicationRequest,
      Publication existingPublication,
      PublicationPermissions permissionStrategy,
      UserInstance userInstance)
      throws ApiGatewayException {
    unpublishPublicationRequest.validate(apiHost);
    permissionStrategy.authorize(UNPUBLISH);

    var updatedPublication =
        toPublicationWithDuplicate(unpublishPublicationRequest, existingPublication, userInstance);
    resourceService.unpublishPublication(updatedPublication, userInstance);
    var updatedResource =
        Resource.resourceQueryObject(updatedPublication.getIdentifier())
            .fetch(resourceService)
            .orElseThrow();
    updateNvaDoi(updatedResource);
    return updatedResource;
  }

  private void updateNvaDoi(Resource resource) {
    if (nonNull(resource.getDoi())) {
      logger.info(
          "Publication {} has NVA-DOI, sending event to EventBridge", resource.getIdentifier());
      var putEventsRequest =
          PutEventsRequest.builder()
              .entries(
                  PutEventsRequestEntry.builder()
                      .eventBusName(nvaEventBusName)
                      .source(NVA_PUBLICATION_DELETE_SOURCE)
                      .detailType(LAMBDA_DESTINATIONS_INVOCATION_RESULT_SUCCESS)
                      .detail(
                          new LambdaDestinationInvocationDetail<>(
                                  DoiMetadataUpdateEvent.createUpdateDoiEvent(
                                      resource.toPublication(), apiHost))
                              .toJsonString())
                      .resources(resource.getIdentifier().toString())
                      .build())
              .build();
      var ebResult = eventBridgeClient.putEvents(putEventsRequest);

      logger.info("failedEntryCount={}", ebResult.failedEntryCount());
    } else {
      logger.info(
          "Publication {} has no NVA-DOI, no event sent to EventBridge", resource.getIdentifier());
    }
  }

  private Publication toPublicationWithDuplicate(
      UnpublishPublicationRequest unpublishPublicationRequest,
      Publication publication,
      UserInstance userInstance) {
    var duplicate = unpublishPublicationRequest.getDuplicateOf().orElse(null);
    var comment = unpublishPublicationRequest.getComment();

    var notes = new ArrayList<>(publication.getPublicationNotes());
    notes.add(
        new UnpublishingNote(comment, new Username(userInstance.getUsername()), Instant.now()));

    return publication.copy().withDuplicateOf(duplicate).withPublicationNotes(notes).build();
  }

  private static boolean canUpdateFileToPendingOpenFile(
      File file,
      CustomerDto customer,
      Resource existingResource,
      Resource updatedResource,
      UserInstance userInstance) {
    var existingFile = existingResource.getFileByIdentifier(file.getIdentifier());
    return existingFile.isEmpty()
        || FileStatus.from(file) != FileStatus.PENDING_OPEN
        || customerAllowsOpenFiles(customer, updatedResource)
        || elevatedUserCanUpdateResource(userInstance);
  }

  private static boolean elevatedUserCanUpdateResource(UserInstance userInstance) {
    return userInstance.getAccessRights().contains(AccessRight.MANAGE_RESOURCES_STANDARD)
        || userInstance.getAccessRights().contains(AccessRight.MANAGE_RESOURCES_ALL);
  }

  private static boolean customerAllowsOpenFiles(CustomerDto customer, Resource resource) {
    var allowedInstanceTypes =
        Optional.ofNullable(customer.allowFileUploadForTypes()).orElse(emptyList());
    return resource.getInstanceType().map(allowedInstanceTypes::contains).orElse(false);
  }

  private static void validateFileTransitions(Resource existingResource, Resource updatedResource)
      throws BadRequestException {
    for (var persistedFile : existingResource.getFiles()) {
      var updatedFile = updatedResource.getFileByIdentifier(persistedFile.getIdentifier());
      if (updatedFile.filter(file -> !persistedFile.canTransitionTo(file)).isPresent()) {
        throw new BadRequestException(
            ILLEGAL_FILE_TRANSITION_MESSAGE.formatted(
                persistedFile.getClass().getSimpleName(),
                updatedFile.orElseThrow().getClass().getSimpleName()));
      }
    }
  }

  private static void authorizeFileEntries(
      Resource existingResource, UserInstance userInstance, List<FileEntry> existingFiles)
      throws UnauthorizedException {
    for (var file : existingFiles) {
      new FilePermissions(file, userInstance, existingResource).authorize(WRITE_METADATA);
    }
  }

  private static List<FileEntry> getUpdatedFiles(
      Resource existingResource, Resource updatedResource) {
    return existingResource.getFileEntries().stream()
        .filter(existingFileEntry -> fileIsPresentAndIsUpdated(updatedResource, existingFileEntry))
        .toList();
  }

  private static boolean fileIsPresentAndIsUpdated(
      Resource updatedResource, FileEntry existingFileEntry) {
    var updatedFile =
        updatedResource.getFileByIdentifier(existingFileEntry.getFile().getIdentifier());
    return updatedFile.filter(file -> isUpdatedFile(existingFileEntry.getFile(), file)).isPresent();
  }

  private static boolean isUpdatedFile(File existingFile, File updatedFile) {
    return !existingFile.equalsExcludingRrsConfiguredType(updatedFile);
  }

  private void setRrsOnFiles(
      Resource updatedResource,
      Resource existingResource,
      CustomerDto customer,
      UserInstance userInstance) {
    new FileRightsRetentionService(
            identityServiceClient, customer.rightsRetentionStrategy(), userInstance)
        .applyRightsRetention(updatedResource, existingResource);
  }

  /** A file that would be approved automatically but has no license. */
  private static ConflictException fileWithoutLicense(FileWithoutLicenseException exception) {
    return new ConflictException(exception, exception.getMessage());
  }

  private static BadGatewayException identityServiceUnavailable(
      IdentityServiceUnavailableException exception) {
    logger.error("Identity service unavailable", exception);
    return new BadGatewayException("Customer API not responding or not responding as expected!");
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

  private boolean identifiersDoNotMatch(SortableIdentifier identifierInPath, UpdateRequest input) {
    return !identifierInPath.equals(input.getIdentifier());
  }

  private void validateRequest(SortableIdentifier identifierInPath, UpdateRequest input)
      throws BadRequestException {
    if (identifiersDoNotMatch(identifierInPath, input)) {
      throw new BadRequestException(IDENTIFIER_MISMATCH_ERROR_MESSAGE);
    }
  }
}
