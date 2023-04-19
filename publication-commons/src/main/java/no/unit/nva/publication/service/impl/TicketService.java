package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.model.business.TicketEntry.setServiceControlledFields;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UntypedTicketQueryObject;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.MessageDao;
import no.unit.nva.publication.model.storage.TicketDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.FunctionWithException;

public class TicketService extends ServiceWithTransactions {

    public static final String TICKET_NOT_FOUND = "Ticket not found";
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;

    private final Supplier<SortableIdentifier> identifierProvider;
    private final ResourceService resourceService;

    public TicketService(AmazonDynamoDB client) {
        this(client, DEFAULT_IDENTIFIER_PROVIDER);
    }

    protected TicketService(AmazonDynamoDB client,
                            Supplier<SortableIdentifier> identifierProvider) {
        super(client);
        this.identifierProvider = identifierProvider;
        resourceService = new ResourceService(client, Clock.systemDefaultZone(), identifierProvider);
    }

    @JacocoGenerated
    public static TicketService defaultService() {
        return new TicketService(DEFAULT_DYNAMODB_CLIENT);
    }

    //TODO make the method protected or package private and use TicketEntry#persist instead.

    /**
     * Method should be protected or package-private.
     *
     * @param ticketEntry the ticket entry to be persisted
     * @param <T>         the TicketEntry class
     * @return the persisted ticket type with service updated fields.
     * @throws ApiGatewayException when an expected error occurs that needs to be sent to the client
     * @deprecated Use TicketEntry#persist instead.
     */
    @Deprecated(since = " TicketEntry#persist")
    public <T extends TicketEntry> T createTicket(TicketEntry ticketEntry)
        throws ApiGatewayException {
        var associatedPublication = fetchPublicationToEnsureItExists(ticketEntry);
        return createTicketForPublication(associatedPublication, ticketEntry);
    }

    public TicketEntry fetchTicket(UserInstance userInstance, SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        var queryObject = TicketEntry.createQueryObject(userInstance, ticketIdentifier);
        return queryObject
                   .fetchTicket(getClient())
                   .map(TicketDao::getData)
                   .map(TicketEntry.class::cast)
                   .orElseThrow(TicketService::notFoundException);
    }

    public TicketEntry fetchTicket(TicketEntry dataEntry)
        throws NotFoundException {
        return fetchTicket(UserInstance.fromTicket(dataEntry), dataEntry.getIdentifier());
    }

    //TODO: should not return anything because we cannot return the persisted entry after a PUT
    // and right now we are returning the input object.
    public TicketEntry updateTicketStatus(TicketEntry ticketEntry, TicketStatus ticketStatus)
        throws ApiGatewayException {
        switch (ticketStatus) {
            case COMPLETED:
                return completeTicket(ticketEntry);
            case CLOSED:
                return closeTicket(ticketEntry);
            default:
                throw new BadRequestException("Cannot update to status " + ticketStatus);
        }
    }

    public <T extends TicketEntry> Optional<T> fetchTicketByResourceIdentifier(URI customerId,
                                                                               SortableIdentifier resourceIdentifier,
                                                                               Class<T> ticketType) {

        TicketDao dao = (TicketDao) TicketEntry.createQueryObject(customerId, resourceIdentifier, ticketType).toDao();
        return dao.fetchByResourceIdentifier(getClient()).map(Dao::getData).map(ticketType::cast);
    }

    public List<Message> fetchTicketMessages(TicketEntry ticketEntry) {
        var dao = (TicketDao) ticketEntry.toDao();
        return dao.fetchTicketMessages(getClient())
                   .map(MessageDao::getData)
                   .map(Message.class::cast)
                   .collect(Collectors.toList());
    }

    public TicketEntry refreshTicket(TicketEntry ticket) {
        var refreshedTicket = ticket.refresh();
        var dao = (TicketDao) refreshedTicket.toDao();
        getClient().putItem(dao.createPutItemRequest());
        return refreshedTicket;
    }

    public TicketEntry fetchTicketForElevatedUser(UserInstance user, SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        var queryObject = TicketEntry.createQueryObject(ticketIdentifier);
        return attempt(() -> queryObject.fetchByIdentifier(getClient()))
                   .map(Dao::getData)
                   .map(TicketEntry.class::cast)
                   .toOptional()
                   .filter(ticketEntry -> ticketEntry.getCustomerId().equals(user.getOrganizationUri()))
                   .orElseThrow(TicketService::notFoundException);
    }

    public Stream<TicketEntry> fetchTicketsForUser(UserInstance userInstance) {
        var queryObject = UntypedTicketQueryObject.create(userInstance);
        return queryObject.fetchTicketsForUser(getClient());
    }

    public TicketEntry fetchTicketByIdentifier(SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        var queryObject = TicketEntry.createQueryObject(ticketIdentifier);
        var queryResult = queryObject.fetchByIdentifier(getClient());
        return (TicketEntry) queryResult.getData();
    }

    public void updateTicket(TicketEntry ticketEntry) {
        ticketEntry.toDao().updateExistingEntry(getClient());
    }

    protected TicketEntry completeTicket(TicketEntry ticketEntry) throws ApiGatewayException {
        var publication = resourceService.getPublicationByIdentifier(ticketEntry.extractPublicationIdentifier());
        var existingTicket =
            attempt(() -> fetchTicketByIdentifier(ticketEntry.getIdentifier()))
                .or(() -> fetchByResourceIdentifierForLegacyDoiRequestsAndPublishingRequests(ticketEntry))
                .orElseThrow(fail -> notFoundException());

        var completed = attempt(() -> existingTicket.complete(publication))
                            .orElseThrow(fail -> handlerTicketUpdateFailure(fail.getException()));

        var putItemRequest = ((TicketDao) completed.toDao()).createPutItemRequest();
        getClient().putItem(putItemRequest);
        return completed;
    }

    protected TicketEntry closeTicket(TicketEntry pendingTicket) throws ApiGatewayException {
        //TODO: can we get both entries at the same time using the single table design?
        resourceService.getPublicationByIdentifier(pendingTicket.extractPublicationIdentifier());
        var persistedTicket = fetchTicketByIdentifier(pendingTicket.getIdentifier());
        var closedTicket = persistedTicket.close();

        var dao = (TicketDao) closedTicket.toDao();
        var putItemRequest = dao.createPutItemRequest();
        getClient().putItem(putItemRequest);
        return closedTicket;
    }

    private static NotFoundException notFoundException() {
        return new NotFoundException(TICKET_NOT_FOUND);
    }

    private ApiGatewayException handlerTicketUpdateFailure(Exception exception) {
        return new BadRequestException(exception.getMessage(), exception);
    }

    //TODO: should try to fetch ticket only by ticket identifier
    private TicketEntry fetchByResourceIdentifierForLegacyDoiRequestsAndPublishingRequests(TicketEntry ticketEntry) {
        return fetchTicketByResourceIdentifier(ticketEntry.getCustomerId(),
                                               ticketEntry.extractPublicationIdentifier(),
                                               ticketEntry.getClass()).orElseThrow();
    }

    private Publication fetchPublicationToEnsureItExists(TicketEntry ticketEntry) throws ForbiddenException {
        var userInstance = UserInstance.create(ticketEntry.getOwner(), ticketEntry.getCustomerId());
        return attempt(() -> resourceService.getPublication(userInstance, ticketEntry.extractPublicationIdentifier()))
                   .orElseThrow(fail -> new ForbiddenException());
    }

    private <T extends TicketEntry> T createTicketForPublication(Publication publication,
                                                                 TicketEntry ticketEntry)
        throws ConflictException {

        setServiceControlledFields(ticketEntry, identifierProvider);
        ticketEntry.validateCreationRequirements(publication);
        var request = ticketEntry.toDao().createInsertionTransactionRequest();
        sendTransactionWriteRequest(request);
        FunctionWithException<TicketEntry, TicketEntry, NotFoundException>
            fetchTicketProvider = this::fetchTicket;
        return (T) fetchEventualConsistentDataEntry(ticketEntry, fetchTicketProvider).orElseThrow();
    }

    public TicketEntry updateTicketAssignee(TicketEntry ticketEntry, User user) throws ApiGatewayException {
         var publication = resourceService.getPublicationByIdentifier(ticketEntry.extractPublicationIdentifier());
        var existingTicket = fetchTicketByIdentifier(ticketEntry.getIdentifier());
        var updatedAssignee = existingTicket.updateAssignee(publication, user);

        var dao = (TicketDao) updatedAssignee.toDao();
        var putItemRequest = dao.createPutItemRequest();
        getClient().putItem(putItemRequest);
        return updatedAssignee;
    }
}