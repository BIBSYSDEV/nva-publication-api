package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static no.unit.nva.publication.RequestUtil.getImportCandidateIdentifier;
import static no.unit.nva.publication.create.ImportCandidateValidator.validate;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.List;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportStatusFactory;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.create.pia.ContributorUpdateService;
import no.unit.nva.publication.create.pia.PiaClient;
import no.unit.nva.publication.exception.NotAuthorizedException;
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
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.CouplingBetweenObjects")
public class CreatePublicationFromImportCandidateHandler extends ApiGatewayHandler<CreatePublicationRequest,
                                                                                      PublicationResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePublicationFromImportCandidateHandler.class);
    public static final String ROLLBACK_WENT_WRONG_MESSAGE = "Rollback went wrong";
    public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
    public static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE = "Resource has already been imported";
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
        this(ImportCandidateHandlerConfigs.getDefaultsConfigs(), new Environment(), TicketService.defaultService(),
             new ApprovalAssignmentServiceForImportCandidateFiles(IdentityServiceClient.prepare()));
    }

    public CreatePublicationFromImportCandidateHandler(
        ImportCandidateHandlerConfigs configs, Environment environment, TicketService ticketService,
        ApprovalAssignmentServiceForImportCandidateFiles approvalService) {
        super(CreatePublicationRequest.class, environment);
        this.candidateService = configs.importCandidateService();
        this.publicationService = configs.publicationService();
        this.associatedArtifactsMover = new AssociatedArtifactsMover(configs.s3Client(),
                                                                     configs.importCandidateStorageBucket(), configs.persistedStorageBucket());
        this.piaClient = new PiaClient(configs.piaClientConfig());
        this.ticketService = ticketService;
        this.approvalService = approvalService;
    }

    @Override
    protected void validateRequest(CreatePublicationRequest importCandidate, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateAccessRight(requestInfo);
    }

    @Override
    protected PublicationResponse processInput(CreatePublicationRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var identifier = getImportCandidateIdentifier(requestInfo);
        var importCandidate = candidateService.getImportCandidateByIdentifier(identifier);
        validate(importCandidate, input);
        return attempt(() -> importCandidate(input, requestInfo, importCandidate))
                   .map(PublicationResponse::fromPublication)
                   .orElseThrow(fail -> rollbackAndThrowException(fail, identifier));
    }

    @Override
    protected Integer getSuccessStatusCode(CreatePublicationRequest input, PublicationResponse output) {
        return HTTP_CREATED;
    }

    private Publication importCandidate(CreatePublicationRequest request, RequestInfo requestInfo, ImportCandidate databaseVersion)
        throws ApiGatewayException, ApprovalAssignmentException {
        var nvaPublication = createNvaPublicationFromImportCandidateAndUserInput(request, requestInfo, databaseVersion);
        candidateService.updateImportStatus(getImportCandidateIdentifier(requestInfo),
                                            ImportStatusFactory.createImported(requestInfo.getUserName(), nvaPublication.getIdentifier()));
        return nvaPublication;
    }

    private Publication createNvaPublicationFromImportCandidateAndUserInput(CreatePublicationRequest request,
                                                                            RequestInfo requestInfo,
                                                                            ImportCandidate databaseVersion)
        throws ApiGatewayException, ApprovalAssignmentException {
        var resourceToImport = ImportCandidateEnricher.createResourceToImport(requestInfo, request, databaseVersion);

        var importedResource = !resourceToImport.getFiles().isEmpty()
                                   ? handleResourceWithFiles(resourceToImport, requestInfo, databaseVersion.getAssociatedCustomers())
                                   : resourceToImport.importResource(publicationService, ImportSource.fromSource(Source.SCOPUS),
                                                                     UserInstance.fromPublication(resourceToImport.toPublication()));

        return finalizeImport(importedResource, databaseVersion);
    }

    private Resource handleResourceWithFiles(Resource resourceToImport, RequestInfo requestInfo,
                                             List<URI> associatedCustomers)
        throws ApiGatewayException, ApprovalAssignmentException {
        var serviceResult = approvalService.determineCustomerResponsibleForApproval(resourceToImport, associatedCustomers);
        LOGGER.info(serviceResult.getReason());

        return switch (serviceResult.getStatus()) {
            case APPROVAL_NEEDED -> createPublicationWithFilesApprovalTicket(resourceToImport, serviceResult, requestInfo);
            case NO_APPROVAL_NEEDED -> resourceToImport.importResource(publicationService,
                                                                       ImportSource.fromSource(Source.SCOPUS),
                                                                       createUserInstanceFromCustomer(requestInfo, serviceResult.getCustomer()));
        };
    }

    private Resource createPublicationWithFilesApprovalTicket(Resource resourceToImport,
                                                              AssignmentServiceResult serviceResult,
                                                              RequestInfo requestInfo)
        throws ApiGatewayException {
        resourceToImport.setAssociatedArtifacts(convertFilesToPending(resourceToImport));
        var fileOwner = createUserInstanceFromCustomer(requestInfo, serviceResult.getCustomer());
        var importedResource = resourceToImport.importResource(publicationService,
                                                               ImportSource.fromSource(Source.SCOPUS),
                                                               fileOwner);
        var fileApproval = PublishingRequestCase.createWithFilesForApproval(
            importedResource,
            fileOwner,
            PublishingWorkflow.lookUp(serviceResult.getCustomer().publicationWorkflow()),
            importedResource.getPendingFiles()
        );
        fileApproval.persistNewTicket(ticketService);
        return importedResource;
    }

    private Publication finalizeImport(Resource importedResource, ImportCandidate rawImportCandidate) {
        var publication = importedResource.toPublication();
        associatedArtifactsMover.moveAssociatedArtifacts(publication, rawImportCandidate);
        new ContributorUpdateService(piaClient).updatePiaContributors(importedResource, rawImportCandidate);
        return publication;
    }

    private static UserInstance createUserInstanceFromCustomer(RequestInfo requestInfo, CustomerDto customer)
        throws UnauthorizedException {
        return UserInstance.create(requestInfo.getUserName(), customer.id(), requestInfo.getPersonCristinId(),
                                   requestInfo.getAccessRights(), customer.cristinId());
    }

    private AssociatedArtifactList convertFilesToPending(Resource resource) {
        var associatedArtifacts = resource.getAssociatedArtifacts().stream()
                                      .map(associatedArtifact -> associatedArtifact instanceof File file ? file.toPendingOpenFile() : associatedArtifact)
                                      .toList();
        return new AssociatedArtifactList(associatedArtifacts);
    }

    private ApiGatewayException rollbackAndThrowException(Failure<PublicationResponse> failure, SortableIdentifier identifier) {
        LOGGER.error("Import failed with exception: {}", failure.getException().getMessage());
        return attempt(() -> rollbackImportStatusUpdate(identifier))
                   .orElse(fail -> throwException(fail.getException()));
    }

    private static ApiGatewayException throwException(Exception exception) {
        if (exception instanceof ApprovalAssignmentException) {
            return new BadRequestException(exception.getMessage());
        }
        return new BadGatewayException(ROLLBACK_WENT_WRONG_MESSAGE);
    }

    private void validateAccessRight(RequestInfo requestInfo) throws NotAuthorizedException {
        if (!requestInfo.userIsAuthorized(AccessRight.MANAGE_IMPORT)) {
            throw new NotAuthorizedException();
        }
    }

    private ApiGatewayException rollbackImportStatusUpdate(SortableIdentifier identifier)
        throws NotFoundException {
        candidateService.updateImportStatus(identifier, ImportStatusFactory.createNotImported());
        return new BadGatewayException(IMPORT_PROCESS_WENT_WRONG);
    }
}
