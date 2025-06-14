package no.unit.nva.publication.ticket.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.TicketStatus.CLOSED;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.utils.RequestUtils.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.utils.RequestUtils.TICKET_IDENTIFIER;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.stream.Collectors;
import no.unit.nva.doi.DataCiteDoiClient;
import no.unit.nva.doi.DoiClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketHandler;
import no.unit.nva.publication.ticket.TicketRequest;
import no.unit.nva.publication.ticket.UpdateTicketOwnershipRequest;
import no.unit.nva.publication.ticket.UpdateTicketRequest;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
public class UpdateTicketHandler extends TicketHandler<TicketRequest, Void> {

    public static final String API_HOST = "API_HOST";
    public static final String EXCEPTION_MESSAGE = "Creating findable doi failed with exception: {}";
    public static final String COULD_NOT_CREATE_FINDABLE_DOI = "Could not create findable doi";
    public static final String PUBLICATION_WITH_IDENTIFIER_S_DOES_NOT_SATISFY_DOI_REQUIREMENTS = "Publication with "
                                                                                                 + "identifier  %s, "
                                                                                                 + "does not satisfy "
                                                                                                 + "DOI requirements";
    public static final String UNKNOWN_VIEWED_STATUS_MESSAGE = "Unknown ViewedStatus";
    private static final Logger logger = LoggerFactory.getLogger(UpdateTicketHandler.class);
    public static final String TICKET_STATUS_UPDATE_MESSAGE =
        "User {} with access rights {} updates ticket status to {} for publication {}";
    public static final String NOT_AUTHORIZED_MESSAGE =
        "User {} is not authorized to manage ticket {} for publication {}";
    private static final String TICKET_ASSIGNEE_UPDATE_MESSAGE =
        "User {} updates assignee to {} for publication {}";
    public static final String UPDATE_FORBIDDEN_MESSAGE =
        "Updating ticket {} for publication {} is forbidden for user {}";
    public static final String FILES_MISSING_MANDATORY_FIELDS_MESSAGE = "Files missing mandatory fields: %s";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final DoiClient doiClient;

    @JacocoGenerated
    public UpdateTicketHandler() {
        this(TicketService.defaultService(),
             ResourceService.defaultService(),
             new DataCiteDoiClient(HttpClient.newHttpClient(), SecretsReader.defaultSecretsManagerClient(),
                                   new Environment().readEnv(API_HOST)),
             new Environment());
    }

    protected UpdateTicketHandler(TicketService ticketService, ResourceService resourceService, DoiClient doiClient,
                                  Environment environment) {
        super(TicketRequest.class, environment);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.doiClient = doiClient;
    }

    @Override
    protected void validateRequest(TicketRequest ticketRequest, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(TicketRequest input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var requestUtils = RequestUtils.fromRequestInfo(requestInfo);
        var ticket = fetchTicket(requestUtils);

        switch (input) {
            case UpdateTicketRequest request -> handleUpdateTicketRequest(ticket, request, requestUtils);
            case UpdateTicketOwnershipRequest request -> updateOwnership(ticket, request, requestUtils);
            default -> {
                // NO OP
            }
        }

        return null;
    }

    private void updateOwnership(TicketEntry ticket, UpdateTicketOwnershipRequest request,
                                 RequestUtils requestUtils) throws ForbiddenException, NotFoundException {
        var resource = getResource(ticket.getResourceIdentifier());
        if (userInstitutionIsReceivingOrganization(ticket, requestUtils)
            && newOwnerAffiliationIsOneOfCuratingInstitutions(request, resource)
            && userIsAuthorized(requestUtils, ticket)) {
            ticket.setReceivingOrganizationDetailsAndResetAssignee(request.ownerAffiliation(),
                                                                   request.responsibilityArea());
            ticket.persistUpdate(ticketService);
        } else {
            throw new ForbiddenException();
        }
    }

    private Resource getResource(SortableIdentifier resourceIdentifier) throws NotFoundException {
        return resourceService.getResourceByIdentifier(resourceIdentifier);
    }

    private static boolean newOwnerAffiliationIsOneOfCuratingInstitutions(UpdateTicketOwnershipRequest request,
                                                                          Resource resource) {
        return resource.getCuratingInstitutions()
                   .stream()
                   .anyMatch(curatingInstitution -> request.ownerAffiliation().equals(curatingInstitution.id()));
    }

    private static boolean userInstitutionIsReceivingOrganization(TicketEntry ticket, RequestUtils requestUtils) {
        return ticket.getReceivingOrganizationDetails()
                   .topLevelOrganizationId()
                   .equals(requestUtils.topLevelCristinOrgId());
    }

    private void handleUpdateTicketRequest(TicketEntry ticket, UpdateTicketRequest request,
                                           RequestUtils requestUtils) throws ApiGatewayException {
        if (hasEffectiveChanges(ticket, request)) {
            updateTicket(request, requestUtils, ticket);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(TicketRequest input, Void output) {
        return HTTP_ACCEPTED;
    }

    private void throwExceptionIfUnauthorized(RequestUtils requestUtils, TicketEntry ticket)
        throws ForbiddenException {
        if (!userIsAuthorized(requestUtils, ticket)) {
            logger.error(NOT_AUTHORIZED_MESSAGE,
                         requestUtils.username(),
                         ticket.getIdentifier(),
                         ticket.getResourceIdentifier());
            throw new ForbiddenException();
        }
    }

    private static boolean statusHasBeenUpdated(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.status()) && !ticketRequest.status().equals(ticket.getStatus());
    }

    private static boolean assigneeHasBeenUpdated(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return incomingAssigneeIsPresent(ticketRequest) && (assigneeDoesNotExist(ticket) || assigneesAreDifferent(
            ticket, ticketRequest));
    }

    private static boolean assigneesAreDifferent(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return !ticketRequest.assignee().equals(ticket.getAssignee());
    }

    private boolean userIsAuthorized(RequestUtils requestUtils, TicketEntry ticket) {
        return requestUtils.isAuthorizedToManage(ticket)
               && ticket.getReceivingOrganizationDetails()
                      .topLevelOrganizationId()
                      .equals(requestUtils.topLevelCristinOrgId());
    }

    private static boolean assigneeDoesNotExist(TicketEntry ticket) {
        return existingAssigneeIsEmpty(ticket);
    }

    private static boolean assigneeIsEmptyString(UpdateTicketRequest ticketRequest) {
        return StringUtils.EMPTY_STRING.equals(ticketRequest.assignee().getValue());
    }

    private static boolean existingAssigneeIsEmpty(TicketEntry ticket) {
        return isNull(ticket.getAssignee());
    }

    private static boolean incomingAssigneeIsPresent(UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.assignee());
    }

    private static boolean userIsTicketAssignee(TicketEntry ticketEntry, String userName) {
        var assignee = ticketEntry.getAssignee();
        return nonNull(assignee) && assignee.toString().equals(userName);
    }

    private void updateTicket(UpdateTicketRequest ticketRequest, RequestUtils requestUtils, TicketEntry ticket)
        throws ApiGatewayException {
        if (incomingUpdateIsStatus(ticket, ticketRequest)) {
            updateStatus(ticketRequest, requestUtils, ticket);
        }
        if (incomingUpdateIsAssignee(ticket, ticketRequest)) {
            logger.info(TICKET_ASSIGNEE_UPDATE_MESSAGE, requestUtils.username(), ticketRequest.assignee(),
                        ticket.getResourceIdentifier());
            updateAssignee(ticketRequest, requestUtils, ticket);
        }
        if (incomingUpdateIsViewedStatus(ticketRequest)) {
            updateTicketViewedBy(ticketRequest, ticket, requestUtils);
        }
    }

    private void updateAssignee(UpdateTicketRequest ticketRequest, RequestUtils requestInfo, TicketEntry ticket)
        throws ApiGatewayException {
        throwExceptionIfUnauthorized(requestInfo, ticket);
        if (assigneeIsEmptyString(ticketRequest)) {
            ticketService.updateTicketAssignee(ticket, null);
        } else {
            ticketService.updateTicketAssignee(ticket, ticketRequest.assignee());
        }
    }

    private void updateStatus(UpdateTicketRequest ticketRequest, RequestUtils requestUtils, TicketEntry ticket)
        throws ApiGatewayException {
        throwExceptionIfUnauthorized(requestUtils, ticket);
        if (ticket instanceof DoiRequest) {
            doiTicketSideEffects(ticketRequest, requestUtils);
        }
        if (ticket instanceof FilesApprovalEntry filesApprovalEntry) {
            fileApprovalEntrySideEffects(filesApprovalEntry, ticketRequest);
        }
        var username = requestUtils.username();
        logger.info(TICKET_STATUS_UPDATE_MESSAGE,
                    username,
                    requestUtils.accessRights(),
                    ticketRequest.status(),
                    ticket.getResourceIdentifier());
        ticketService.updateTicketStatus(ticket, ticketRequest.status(), requestUtils.toUserInstance());
    }

    private void fileApprovalEntrySideEffects(FilesApprovalEntry ticket,
                                              UpdateTicketRequest ticketRequest) throws ConflictException {

        if (COMPLETED.equals(ticketRequest.status())) {
            validateFilesForApproval(ticket);
            ticket.approveFiles().persistUpdate(ticketService);
        }
    }

    private void validateFilesForApproval(FilesApprovalEntry ticket) throws ConflictException {
        if (filesForApprovalAreNotApprovable(ticket)) {
            var fileIdsMissingMandatoryFields = getFileIdsMissingMandatoryFields(ticket);
            throw new ConflictException(FILES_MISSING_MANDATORY_FIELDS_MESSAGE
                                            .formatted(fileIdsMissingMandatoryFields));
        }
    }

    private static String getFileIdsMissingMandatoryFields(FilesApprovalEntry ticket) {
        return ticket.getFilesForApproval().stream()
                   .map(PendingFile.class::cast)
                   .filter(PendingFile::isNotApprovable)
                   .map(File.class::cast)
                   .map(File::getIdentifier)
                   .map(String::valueOf)
                   .collect(Collectors.joining(","));
    }

    private static boolean filesForApprovalAreNotApprovable(FilesApprovalEntry ticket) {
        return ticket.getFilesForApproval().stream()
                   .map(PendingFile.class::cast)
                   .anyMatch(PendingFile::isNotApprovable);
    }

    private TicketEntry fetchTicket(RequestUtils requestUtils)
        throws ForbiddenException {
        return attempt(requestUtils::ticketIdentifier)
                   .map(ticketService::fetchTicketByIdentifier)
                   .orElseThrow(fail -> getForbiddenException(requestUtils));
    }

    private static ForbiddenException getForbiddenException(RequestUtils requestUtils) {
        var publicationIdentifier = requestUtils.pathParameters().get(PUBLICATION_IDENTIFIER);
        var ticketIdentifier = requestUtils.pathParameters().get(TICKET_IDENTIFIER);
        logger.info(UPDATE_FORBIDDEN_MESSAGE, ticketIdentifier, publicationIdentifier, requestUtils.username());
        return new ForbiddenException();
    }

    private void updateTicketViewedBy(UpdateTicketRequest ticketRequest, TicketEntry ticket, RequestUtils requestUtils)
        throws ApiGatewayException {
        assertThatPublicationIdentifierInPathReferencesCorrectPublication(ticket, requestUtils);
        if (userIsTicketAssignee(ticket, requestUtils.username())) {
            throwExceptionIfUnauthorized(requestUtils, ticket);
            markTicketForAssignee(ticketRequest, ticket);
        } else if (userIsTicketOwner(ticket, requestUtils.username())) {
            markTicketForOwner(ticketRequest, ticket);
        } else if (requestUtils.isAuthorizedToManage(ticket)) {
            markTicketForCurator(ticketRequest, ticket, requestUtils.username());
        }
    }

    private void markTicketForCurator(UpdateTicketRequest ticketRequest, TicketEntry ticket, String userName) {
        if (ViewStatus.READ.equals(ticketRequest.viewStatus())) {
            ticket.markReadByUser(new User(userName)).persistUpdate(ticketService);
        } else if (ViewStatus.UNREAD.equals(ticketRequest.viewStatus())) {
            ticket.markReadByUser(new User(userName)).persistUpdate(ticketService);
        } else {
            throw new UnsupportedOperationException(UNKNOWN_VIEWED_STATUS_MESSAGE);
        }
    }

    private boolean userIsTicketOwner(TicketEntry ticket, String userName) {
        var owner = ticket.getOwner();
        return nonNull(owner) && owner.toString().equals(userName);
    }

    private void markTicketForOwner(UpdateTicketRequest input, TicketEntry ticket) {
        if (ViewStatus.READ.equals(input.viewStatus())) {
            ticket.markReadByOwner().persistUpdate(ticketService);
        } else if (ViewStatus.UNREAD.equals(input.viewStatus())) {
            ticket.markUnreadByOwner().persistUpdate(ticketService);
        } else {
            throw new UnsupportedOperationException(UNKNOWN_VIEWED_STATUS_MESSAGE);
        }
    }

    private void markTicketForAssignee(UpdateTicketRequest input, TicketEntry ticket) {

        if (ViewStatus.READ.equals(input.viewStatus())) {
            ticket.markReadForAssignee().persistUpdate(ticketService);
        } else if (ViewStatus.UNREAD.equals(input.viewStatus())) {
            ticket.markUnReadForAssignee().persistUpdate(ticketService);
        } else {
            throw new UnsupportedOperationException(UNKNOWN_VIEWED_STATUS_MESSAGE);
        }
    }

    private void assertThatPublicationIdentifierInPathReferencesCorrectPublication(TicketEntry ticket,
                                                                                   RequestUtils requestUtils)
        throws ForbiddenException, NotFoundException {
        var suppliedPublicationIdentifier =
            requestUtils.publicationIdentifier();
        if (!suppliedPublicationIdentifier.equals(ticket.getResourceIdentifier())) {
            throw new ForbiddenException();
        }
    }

    private boolean incomingUpdateIsViewedStatus(UpdateTicketRequest input) {
        return nonNull(input.viewStatus());
    }

    private boolean incomingUpdateIsAssignee(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.assignee()) && !ticketRequest.assignee().equals(ticket.getAssignee());
    }

    private boolean incomingUpdateIsStatus(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.status()) && !ticket.getStatus().equals(ticketRequest.status());
    }

    private boolean hasEffectiveChanges(TicketEntry ticket, UpdateTicketRequest ticketRequest) {
        return statusHasBeenUpdated(ticket, ticketRequest)
               || assigneeHasBeenUpdated(ticket, ticketRequest)
               || viewStatusHasBeenUpdated(ticketRequest);
    }

    private boolean viewStatusHasBeenUpdated(UpdateTicketRequest ticketRequest) {
        return nonNull(ticketRequest.viewStatus());
    }

    private void doiTicketSideEffects(UpdateTicketRequest input, final RequestUtils requestUtils)
        throws NotFoundException, BadMethodException, BadGatewayException {
        var status = input.status();
        var publication = getResource(requestUtils.publicationIdentifier()).toPublication();
        if (TicketStatus.COMPLETED.equals(status)) {
            findableDoiTicketSideEffects(publication);
        }
        if (CLOSED.equals(status) && hasDoi(publication)) {
            deleteDoiTicketSideEffects(publication);
        }
    }

    private void findableDoiTicketSideEffects(Publication publication)
        throws BadMethodException, BadGatewayException {
        publicationSatisfiesDoiRequirements(publication);
        createFindableDoiAndPersistDoiOnPublication(publication);
    }

    private void createFindableDoiAndPersistDoiOnPublication(Publication publication) throws BadGatewayException {
        try {
            var doi = doiClient.createFindableDoi(publication);
            updatePublication(publication, doi);
        } catch (Exception e) {
            logger.error(EXCEPTION_MESSAGE, e.getMessage());
            throw new BadGatewayException(COULD_NOT_CREATE_FINDABLE_DOI);
        }
    }

    private boolean hasDoi(Publication publication) {
        return nonNull(publication.getDoi());
    }

    private void deleteDoiTicketSideEffects(Publication publication) {
        doiClient.deleteDraftDoi(publication);
        publication.setDoi(null);
        resourceService.updatePublication(publication);
    }

    private void updatePublication(Publication publication, URI doi) {
        if (isNull(publication.getDoi())) {
            publication.setDoi(doi);
            resourceService.updatePublication(publication);
        }
    }

    private void publicationSatisfiesDoiRequirements(Publication publication) throws BadMethodException {
        if (!publication.satisfiesFindableDoiRequirements()) {
            throw new BadMethodException(String.format(PUBLICATION_WITH_IDENTIFIER_S_DOES_NOT_SATISFY_DOI_REQUIREMENTS,
                                                       publication.getIdentifier()));
        }
    }
}
