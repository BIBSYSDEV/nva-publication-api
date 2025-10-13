package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.util.function.Predicate.not;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
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
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePublicationFromImportCandidateHandler extends ApiGatewayHandler<ImportCandidate,
                                                                                      PublicationResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePublicationFromImportCandidateHandler.class);
    public static final String SCOPUS_IDENTIFIER = "Scopus";
    public static final String ROLLBACK_WENT_WRONG_MESSAGE = "Rollback went wrong";
    public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
    public static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE = "Resource has already been imported";
    public static final String RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE =
        "Resource is missing scopus identifier";
    public static final String RESOURCE_IS_NOT_PUBLISHABLE = "Resource is not publishable";

    private final ResourceService candidateService;
    private final ResourceService publicationService;
    private final TicketService ticketService;
    private final IdentityServiceClient identityServiceClient;
    private final PiaClient piaClient;
    private final AssociatedArtifactsMover associatedArtifactsMover;

    @JacocoGenerated
    public CreatePublicationFromImportCandidateHandler() {
        this(ImportCandidateHandlerConfigs.getDefaultsConfigs(), new Environment(), TicketService.defaultService(),
             IdentityServiceClient.prepare());
    }

    public CreatePublicationFromImportCandidateHandler(
        ImportCandidateHandlerConfigs configs, Environment environment, TicketService ticketService, IdentityServiceClient identityServiceClient) {
        super(ImportCandidate.class, environment);
        this.candidateService = configs.importCandidateService();
        this.publicationService = configs.publicationService();
        this.associatedArtifactsMover = new AssociatedArtifactsMover(configs.s3Client(),
                                                                     configs.importCandidateStorageBucket(), configs.persistedStorageBucket());
        this.piaClient = new PiaClient(configs.piaClientConfig());
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
    }

    @Override
    protected void validateRequest(ImportCandidate importCandidate, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validateAccessRight(requestInfo);
        ImportCandidateValidator.validate(importCandidate);
    }

    @Override
    protected PublicationResponse processInput(ImportCandidate input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return attempt(() -> importCandidate(input, requestInfo))
                   .map(PublicationResponse::fromPublication)
                   .orElseThrow(fail -> rollbackAndThrowException(fail, input));
    }

    @Override
    protected Integer getSuccessStatusCode(ImportCandidate input, PublicationResponse output) {
        return HTTP_CREATED;
    }

    private Publication importCandidate(ImportCandidate input, RequestInfo requestInfo)
        throws ApiGatewayException {
        var nvaPublication = createNvaPublicationFromImportCandidateAndUserInput(input, requestInfo);
        candidateService.updateImportStatus(input.getIdentifier(),
                                            ImportStatusFactory.createImported(requestInfo.getUserName(), nvaPublication.getIdentifier()));
        return nvaPublication;
    }

    private Publication createNvaPublicationFromImportCandidateAndUserInput(ImportCandidate importCandidate,
                                                                            RequestInfo requestInfo)
        throws ApiGatewayException {
        var rawImportCandidate = candidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
        var resourceToImport = ImportCandidateEnricher.createResourceToImport(requestInfo, importCandidate,
                                                                              rawImportCandidate);

        if (!resourceToImport.getFiles().isEmpty()) {
            var customer = customerRequiringFileApproval(importCandidate);
            if (customer.isPresent()) {
                resourceToImport.setAssociatedArtifacts(convertFilesToPending(resourceToImport));
                var importedResource = resourceToImport.importResource(publicationService, ImportSource.fromSource(Source.SCOPUS));
                var userInstanceFromCustomer = createUserInstanceFromCustomer(requestInfo, customer.get());
                var fileApproval = PublishingRequestCase.createWithFilesForApproval(
                    importedResource,
                    userInstanceFromCustomer,
                    PublishingWorkflow.lookUp(customer.get().publicationWorkflow()),
                    importedResource.getPendingFiles()
                );
                fileApproval.persistNewTicket(ticketService);
                return finalizeImport(importedResource, importCandidate, rawImportCandidate);
            }
        }

        var importedResource = resourceToImport.importResource(publicationService, ImportSource.fromSource(Source.SCOPUS));
        return finalizeImport(importedResource, importCandidate, rawImportCandidate);
    }

    private Publication finalizeImport(Resource importedResource, ImportCandidate importCandidate, ImportCandidate rawImportCandidate) {
        var publication = importedResource.toPublication();
        associatedArtifactsMover.moveAssociatedArtifacts(publication, rawImportCandidate);
        new ContributorUpdateService(piaClient).updatePiaContributors(importCandidate, rawImportCandidate);
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

    private Optional<CustomerDto> customerRequiringFileApproval(ImportCandidate importCandidate) {
        return importCandidate.getAssociatedCustomers().stream()
                   .map(uri -> attempt(() -> identityServiceClient.getCustomerById(uri)).orElseThrow())
                   .filter(not(CustomerDto::autoPublishScopusImportFiles))
                   .findFirst();
    }

    private BadGatewayException rollbackAndThrowException(Failure<PublicationResponse> failure, ImportCandidate input) {
        LOGGER.error("Import failed with exception: {}", failure.getException().getMessage());
        return attempt(() -> rollbackImportStatusUpdate(input))
                   .orElse(fail -> new BadGatewayException(ROLLBACK_WENT_WRONG_MESSAGE));
    }

    private void validateAccessRight(RequestInfo requestInfo) throws NotAuthorizedException {
        if (!requestInfo.userIsAuthorized(AccessRight.MANAGE_IMPORT)) {
            throw new NotAuthorizedException();
        }
    }

    private BadGatewayException rollbackImportStatusUpdate(ImportCandidate importCandidate)
        throws NotFoundException {
        candidateService.updateImportStatus(importCandidate.getIdentifier(), ImportStatusFactory.createNotImported());
        return new BadGatewayException(IMPORT_PROCESS_WENT_WRONG);
    }
}
