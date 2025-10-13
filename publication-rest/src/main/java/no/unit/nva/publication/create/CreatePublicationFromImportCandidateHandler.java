package no.unit.nva.publication.create;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.util.function.Predicate.not;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
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
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
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
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreatePublicationFromImportCandidateHandler extends ApiGatewayHandler<ImportCandidate,
                                                                                      PublicationResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePublicationFromImportCandidateHandler.class);
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String SCOPUS_IDENTIFIER = "Scopus";
    public static final String ROLLBACK_WENT_WRONG_MESSAGE = "Rollback went wrong";
    public static final String IMPORT_PROCESS_WENT_WRONG = "Import process went wrong";
    public static final String RESOURCE_HAS_ALREADY_BEEN_IMPORTED_ERROR_MESSAGE = "Resource has already been imported";
    public static final String RESOURCE_IS_MISSING_SCOPUS_IDENTIFIER_ERROR_MESSAGE =
        "Resource is missing scopus identifier";
    public static final String PUBLICATION = "publication";
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

    private static ImportStatus toImportStatus(String username, URI uri) {
        return ImportStatusFactory.createImported(new Username(username), uri);
    }

    private static boolean notAuthorizedToProcessImportCandidates(RequestInfo requestInfo) {
        return !requestInfo.userIsAuthorized(AccessRight.MANAGE_IMPORT);
    }

    private URI toPublicationUriIdentifier(Publication publication) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION)
                   .addChild(publication.getIdentifier().toString())
                   .getUri();
    }

    private Publication importCandidate(ImportCandidate input, RequestInfo requestInfo)
        throws ApiGatewayException {
        var nvaPublication = createNvaPublicationFromImportCandidateAndUserInput(input, requestInfo);
        candidateService.updateImportStatus(input.getIdentifier(), toImportStatus(requestInfo.getUserName(), toPublicationUriIdentifier(nvaPublication)));
        return nvaPublication;
    }

    private Publication createNvaPublicationFromImportCandidateAndUserInput(ImportCandidate importCandidate,
                                                                            RequestInfo requestInfo)
        throws ApiGatewayException {
        var rawImportCandidate = candidateService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
        var importCandidateToImport = updateCandidateFromRequest(requestInfo, importCandidate, rawImportCandidate);
        var resourceToImport = Resource.fromImportCandidate(importCandidateToImport);

        if (hasFiles(resourceToImport)) {
            var customer = customerRequiringFileApproval(importCandidate);
            if (customer.isPresent()) {
                resourceToImport.setAssociatedArtifacts(convertFilesToPending(resourceToImport));
                var importedResource = resourceToImport.importResource(publicationService, ImportSource.fromSource(Source.SCOPUS));
                createAndPersistFileApprovalTicket(requestInfo, customer.get(), importedResource);
                return finalizeImport(importedResource, importCandidate, rawImportCandidate);
            }
        }

        var importedResource = resourceToImport.importResource(publicationService, ImportSource.fromSource(Source.SCOPUS));
        return finalizeImport(importedResource, importCandidate, rawImportCandidate);
    }

    private void createAndPersistFileApprovalTicket(RequestInfo requestInfo, CustomerDto customer,
                                                    Resource importedResource)
        throws ApiGatewayException {
        var userInstanceFromCustomer = createUserInstanceFromCustomer(requestInfo, customer);
        var fileApproval = PublishingRequestCase.createWithFilesForApproval(
            importedResource,
            userInstanceFromCustomer,
            PublishingWorkflow.lookUp(customer.publicationWorkflow()),
            importedResource.getPendingFiles()
        );
        fileApproval.persistNewTicket(ticketService);
    }

    private Publication finalizeImport(Resource importedResource, ImportCandidate importCandidate, ImportCandidate rawImportCandidate) {
        var publication = importedResource.toPublication();
        associatedArtifactsMover.moveAssociatedArtifacts(publication, rawImportCandidate);
        new ContributorUpdateService(piaClient).updatePiaContributors(importCandidate, rawImportCandidate);
        return publication;
    }

    private static UserInstance createUserInstanceFromCustomer(RequestInfo requestInfo, CustomerDto customer)
        throws UnauthorizedException {
        var resourceOwner = new ResourceOwner(new Username(requestInfo.getUserName()), customer.cristinId());
        return UserInstance.create(resourceOwner, customer.id());
    }

    private AssociatedArtifactList convertFilesToPending(Resource resource) {
        var associatedArtifacts = resource.getAssociatedArtifacts().stream()
                                      .map(associatedArtifact -> associatedArtifact instanceof File file ? file.toPendingOpenFile() : associatedArtifact)
                                      .toList();
        return new AssociatedArtifactList(associatedArtifacts);
    }

    private static boolean hasFiles(Resource resourceToImport) {
        return !resourceToImport.getFiles().isEmpty();
    }

    private Optional<CustomerDto> customerRequiringFileApproval(ImportCandidate importCandidate) {
        return importCandidate.getAssociatedCustomers().stream()
                   .map(uri -> attempt(() -> identityServiceClient.getCustomerById(uri)).orElseThrow())
                   .filter(not(CustomerDto::autoPublishScopusImportFiles))
                   .findFirst();
    }

    private ImportCandidate updateCandidateFromRequest(RequestInfo requestInfo,
                                                       ImportCandidate userInput,
                                                       ImportCandidate databaseVersion)
        throws UnauthorizedException {
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        return databaseVersion.copyImportCandidate()
                   .withEntityDescription(userInput.getEntityDescription())
                   .withAssociatedArtifacts(userInput.getAssociatedArtifacts())
                   .withDoi(userInput.getDoi())
                   .withAdditionalIdentifiers(userInput.getAdditionalIdentifiers())
                   .withProjects(userInput.getProjects())
                   .withSubjects(userInput.getSubjects())
                   .withFundings(userInput.getFundings())
                   .withRightsHolder(userInput.getRightsHolder())
                   .withHandle(userInput.getHandle())
                   .withLink(userInput.getLink())
                   .withPublisher(Organization.fromUri(userInstance.getCustomerId()))
                   .withResourceOwner(new ResourceOwner(new Username(userInstance.getUsername()),
                                                        userInstance.getTopLevelOrgCristinId()))
                   .build();
    }

    private BadGatewayException rollbackAndThrowException(Failure<PublicationResponse> failure, ImportCandidate input) {
        LOGGER.error("Import failed with exception: {}", failure.getException().getMessage());
        return attempt(() -> rollbackImportStatusUpdate(input))
                   .orElse(fail -> new BadGatewayException(ROLLBACK_WENT_WRONG_MESSAGE));
    }

    private void validateAccessRight(RequestInfo requestInfo) throws NotAuthorizedException {
        if (notAuthorizedToProcessImportCandidates(requestInfo)) {
            throw new NotAuthorizedException();
        }
    }

    private BadGatewayException rollbackImportStatusUpdate(ImportCandidate importCandidate)
        throws NotFoundException {
        candidateService.updateImportStatus(importCandidate.getIdentifier(), ImportStatusFactory.createNotImported());
        return new BadGatewayException(IMPORT_PROCESS_WENT_WRONG);
    }
}
