package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static no.unit.nva.publication.RequestUtil.getImportCandidateIdentifier;
import static no.unit.nva.publication.create.ImportCandidateValidator.validate;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.publication.create.pia.ContributorUpdateService;
import no.unit.nva.publication.create.pia.PiaClient;
import no.unit.nva.publication.exception.NotAuthorizedException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ApprovalAssignmentServiceForImportCandidateFiles;
import no.unit.nva.publication.service.impl.ApprovalAssignmentServiceForImportCandidateFiles.ApprovalAssignmentException;
import no.unit.nva.publication.service.impl.ApprovalAssignmentServiceForImportCandidateFiles.AssignmentServiceResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class CreatePublicationFromImportCandidateHandler
    extends ApiGatewayHandler<CreatePublicationRequest, PublicationResponse> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CreatePublicationFromImportCandidateHandler.class);
  private static final String LOG_PROCESSING_IMPORT_CANDIDATE =
      "Starting import of import candidate {}";
  private static final String LOG_PUBLICATION_CREATED =
      "Publication {} created from import candidate {}";
  private static final String LOG_IMPORT_STATUS_UPDATED =
      "Import candidate {} set to IMPORTED, publication {}";
  public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
  public static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE =
      "Resource has already been imported";
  public static final String RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE =
      "Resource is missing scopus identifier";
  public static final String RESOURCE_IS_NOT_PUBLISHABLE = "Resource is not publishable";

  private final ResourceService candidateService;
  private final ResourceService publicationService;
  private final TicketService ticketService;
  private final ApprovalAssignmentServiceForImportCandidateFiles approvalService;
  private final PiaClient piaClient;
  private final AssociatedArtifactsMover associatedArtifactsMover;

  @JacocoGenerated
  public CreatePublicationFromImportCandidateHandler() {
    this(
        ImportCandidateHandlerConfigs.getDefaultsConfigs(),
        new Environment(),
        TicketService.defaultService(),
        new ApprovalAssignmentServiceForImportCandidateFiles(IdentityServiceClient.prepare()));
  }

  public CreatePublicationFromImportCandidateHandler(
      ImportCandidateHandlerConfigs configs,
      Environment environment,
      TicketService ticketService,
      ApprovalAssignmentServiceForImportCandidateFiles approvalService) {
    super(CreatePublicationRequest.class, environment);
    this.candidateService = configs.importCandidateService();
    this.publicationService = configs.publicationService();
    this.associatedArtifactsMover =
        new AssociatedArtifactsMover(
            configs.s3Client(),
            configs.importCandidateStorageBucket(),
            configs.persistedStorageBucket());
    this.piaClient = new PiaClient(configs.piaClientConfig());
    this.ticketService = ticketService;
    this.approvalService = approvalService;
  }

  @Override
  protected void validateRequest(
      CreatePublicationRequest importCandidate, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    validateAccessRight(requestInfo);
  }

  @Override
  protected PublicationResponse processInput(
      CreatePublicationRequest input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var identifier = getImportCandidateIdentifier(requestInfo);
    LOGGER.info(LOG_PROCESSING_IMPORT_CANDIDATE, identifier);
    var importCandidate = candidateService.getImportCandidateByIdentifier(identifier);
    validate(importCandidate, input);
    try {
      var publication = importCandidate(input, requestInfo, importCandidate);
      return PublicationResponse.fromPublication(publication);
    } catch (ApprovalAssignmentException e) {
      throw new BadRequestException(e.getMessage());
    } catch (TransactionFailedException e) {
      throw new BadGatewayException(IMPORT_PROCESS_WENT_WRONG);
    }
  }

  @Override
  protected Integer getSuccessStatusCode(
      CreatePublicationRequest input, PublicationResponse output) {
    return HTTP_CREATED;
  }

  private Publication importCandidate(
      CreatePublicationRequest request, RequestInfo requestInfo, ImportCandidate databaseVersion)
      throws ApiGatewayException, ApprovalAssignmentException {
    var nvaPublication =
        createNvaPublicationFromImportCandidateAndUserInput(request, requestInfo, databaseVersion);
    var publicationIdentifier = nvaPublication.getIdentifier();
    var candidateIdentifier = databaseVersion.getIdentifier();
    LOGGER.info(LOG_PUBLICATION_CREATED, publicationIdentifier, candidateIdentifier);
    LOGGER.info(LOG_IMPORT_STATUS_UPDATED, candidateIdentifier, publicationIdentifier);
    return nvaPublication;
  }

  private Publication createNvaPublicationFromImportCandidateAndUserInput(
      CreatePublicationRequest request, RequestInfo requestInfo, ImportCandidate databaseVersion)
      throws ApiGatewayException, ApprovalAssignmentException {
    var resourceToImport =
        ImportCandidateEnricher.createResourceToImport(requestInfo, request, databaseVersion);

    var importedResource =
        !resourceToImport.getFiles().isEmpty()
            ? handleResourceWithFiles(resourceToImport, requestInfo, databaseVersion)
            : importResource(
                resourceToImport,
                UserInstance.fromPublication(resourceToImport.toPublication()),
                databaseVersion.getIdentifier(),
                requestInfo.getUserName());
    return finalizeImport(importedResource, databaseVersion);
  }

  private Resource importResource(
      Resource resourceToImport,
      UserInstance fileOwner,
      SortableIdentifier candidateIdentifier,
      String userName)
      throws NotFoundException {
    return resourceToImport.importResourceAndUpdateImportCandidateStatus(
        publicationService,
        ImportSource.fromSource(Source.SCOPUS),
        fileOwner,
        candidateService,
        candidateIdentifier,
        userName);
  }

  private Resource handleResourceWithFiles(
      Resource resourceToImport, RequestInfo requestInfo, ImportCandidate databaseVersion)
      throws ApiGatewayException, ApprovalAssignmentException {
    var serviceResult =
        approvalService.determineCustomerResponsibleForApproval(
            resourceToImport, databaseVersion.getAssociatedCustomers());
    LOGGER.info(serviceResult.getReason());
    var fileOwner = createUserInstanceFromCustomer(requestInfo, serviceResult.getCustomer());

    return switch (serviceResult.getStatus()) {
      case APPROVAL_NEEDED ->
          createPublicationWithFilesApprovalTicket(
              resourceToImport, serviceResult, requestInfo, databaseVersion, fileOwner);
      case NO_APPROVAL_NEEDED ->
          importResourceWithInternalFiles(
              resourceToImport, databaseVersion, requestInfo, fileOwner);
    };
  }

  private Resource importResourceWithInternalFiles(
      Resource resourceToImport,
      ImportCandidate databaseVersion,
      RequestInfo requestInfo,
      UserInstance fileOwner)
      throws NotFoundException, UnauthorizedException {
    resourceToImport.setAssociatedArtifacts(convertFilesToInternalFiles(resourceToImport));
    return importResource(
        resourceToImport, fileOwner, databaseVersion.getIdentifier(), requestInfo.getUserName());
  }

  private static AssociatedArtifactList convertFilesToInternalFiles(Resource resourceToImport) {
    var associatedArtifacts =
        resourceToImport.getAssociatedArtifacts().stream()
            .map(CreatePublicationFromImportCandidateHandler::toInternalFile)
            .toList();
    return new AssociatedArtifactList(associatedArtifacts);
  }

  private static AssociatedArtifact toInternalFile(AssociatedArtifact associatedArtifact) {
    return associatedArtifact instanceof File file ? file.toInternalFile() : associatedArtifact;
  }

  private Resource createPublicationWithFilesApprovalTicket(
      Resource resourceToImport,
      AssignmentServiceResult serviceResult,
      RequestInfo requestInfo,
      ImportCandidate databaseVersion,
      UserInstance fileOwner)
      throws ApiGatewayException {
    resourceToImport.setAssociatedArtifacts(convertFilesToPending(resourceToImport));
    var importedResource =
        importResource(
            resourceToImport,
            fileOwner,
            databaseVersion.getIdentifier(),
            requestInfo.getUserName());
    var fileApproval =
        PublishingRequestCase.createWithFilesForApproval(
            importedResource,
            fileOwner,
            PublishingWorkflow.lookUp(serviceResult.getCustomer().publicationWorkflow()),
            importedResource.getPendingFiles());
    fileApproval.persistNewTicket(ticketService);
    return importedResource;
  }

  private Publication finalizeImport(
      Resource importedResource, ImportCandidate rawImportCandidate) {
    var publication = importedResource.toPublication();
    associatedArtifactsMover.moveAssociatedArtifacts(publication, rawImportCandidate);
    new ContributorUpdateService(piaClient)
        .updatePiaContributors(importedResource, rawImportCandidate);
    return publication;
  }

  private static UserInstance createUserInstanceFromCustomer(
      RequestInfo requestInfo, CustomerDto customer) throws UnauthorizedException {
    return UserInstance.create(
        requestInfo.getUserName(),
        customer.id(),
        requestInfo.getPersonCristinId(),
        requestInfo.getAccessRights(),
        customer.cristinId());
  }

  private AssociatedArtifactList convertFilesToPending(Resource resource) {
    var associatedArtifacts =
        resource.getAssociatedArtifacts().stream()
            .map(CreatePublicationFromImportCandidateHandler::toPendingFile)
            .toList();
    return new AssociatedArtifactList(associatedArtifacts);
  }

  private static AssociatedArtifact toPendingFile(AssociatedArtifact associatedArtifact) {
    return switch (associatedArtifact) {
      case InternalFile file -> file.toPendingInternalFile();
      case OpenFile file -> file.toPendingOpenFile();
      default -> associatedArtifact;
    };
  }

  private void validateAccessRight(RequestInfo requestInfo) throws NotAuthorizedException {
    if (!requestInfo.userIsAuthorized(AccessRight.MANAGE_IMPORT)) {
      throw new NotAuthorizedException();
    }
  }
}
