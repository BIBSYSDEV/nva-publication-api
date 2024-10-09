package no.unit.nva.publication.service.impl;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.model.business.TicketEntry.setServiceControlledFields;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UntypedTicketQueryObject;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketService extends ServiceWithTransactions {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    public static final String TICKET_NOT_FOUND = "Ticket not found";
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;
    private static final String TICKET_REFRESHED_MESSAGE = "Ticket has been refreshed successfully: {}";
    public static final String TICKET_TO_REFRESH_NOT_FOUND_MESSAGE = "Ticket to refresh is not found: {}";
    private final Supplier<SortableIdentifier> identifierProvider;
    private final ResourceService resourceService;
    private final String tableName;

    public TicketService(AmazonDynamoDB client, RawContentRetriever uriRetriever) {
        this(client, DEFAULT_IDENTIFIER_PROVIDER, uriRetriever);
    }

    protected TicketService(AmazonDynamoDB client,
                            Supplier<SortableIdentifier> identifierProvider,
                            RawContentRetriever uriRetriever) {
        super(client);
        this.identifierProvider = identifierProvider;
        tableName = RESOURCES_TABLE_NAME;
        resourceService = ResourceService.builder()
                              .withDynamoDbClient(client)
                              .withIdentifierSupplier(identifierProvider)
                              .withUriRetriever(uriRetriever)
                              .build();
    }

    @JacocoGenerated
    public static TicketService defaultService() {
        return new TicketService(DEFAULT_DYNAMODB_CLIENT, UriRetriever.defaultUriRetriever());
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
    public TicketEntry updateTicketStatus(TicketEntry ticketEntry, TicketStatus ticketStatus, Username finalizedBy)
        throws ApiGatewayException {
        switch (ticketStatus) {
            case COMPLETED:
                return completeTicket(ticketEntry, finalizedBy);
            case CLOSED:
                return closeTicket(ticketEntry, finalizedBy);
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
        return attempt(() -> queryObject.fetchByIdentifier(getClient(), tableName))
                   .map(Dao::getData)
                   .map(TicketEntry.class::cast)
                   .toOptional()
                   .filter(ticketEntry -> ticketEntry.getCustomerId().equals(user.getCustomerId()))
                   .orElseThrow(TicketService::notFoundException);
    }

    public Stream<TicketEntry> fetchTicketsForUser(UserInstance userInstance) {
        var queryObject = UntypedTicketQueryObject.create(userInstance);
        return queryObject.fetchTicketsForUser(getClient());
    }

    public TicketEntry fetchTicketByIdentifier(SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        var queryObject = TicketEntry.createQueryObject(ticketIdentifier);
        var queryResult = queryObject.fetchByIdentifier(getClient(), tableName);
        return (TicketEntry) queryResult.getData();
    }

    public void updateTicket(TicketEntry ticketEntry) {
        ticketEntry.toDao().updateExistingEntry(getClient());
    }

    public TicketEntry updateTicketAssignee(TicketEntry ticketEntry, Username assignee) throws ApiGatewayException {
        var publication = resourceService.getPublicationByIdentifier(ticketEntry.getResourceIdentifier());
        var existingTicket = fetchTicketByIdentifier(ticketEntry.getIdentifier());
        var updatedAssignee = existingTicket.updateAssignee(publication, assignee);

        var dao = (TicketDao) updatedAssignee.toDao();
        var putItemRequest = dao.createPutItemRequest();
        getClient().putItem(putItemRequest);
        return updatedAssignee;
    }

    public void refresh(SortableIdentifier identifier) {
        try {
            var ticket = fetchTicketByIdentifier(identifier);
            ticket.persistUpdate(this);
            logger.info(TICKET_REFRESHED_MESSAGE, identifier);
        } catch (NotFoundException e) {
            logger.error(TICKET_TO_REFRESH_NOT_FOUND_MESSAGE, identifier);
        }
    }

    protected TicketEntry completeTicket(TicketEntry ticketEntry, Username username) throws ApiGatewayException {
        var publication = resourceService.getPublicationByIdentifier(ticketEntry.getResourceIdentifier());
        var existingTicket =
            attempt(() -> fetchTicketByIdentifier(ticketEntry.getIdentifier()))
                .or(() -> fetchByResourceIdentifierForLegacyDoiRequestsAndPublishingRequests(ticketEntry))
                .orElseThrow(fail -> notFoundException());
        injectAssigneeWhenUnassigned(existingTicket, username);
        var completed = attempt(() -> existingTicket.complete(publication, username))
                            .orElseThrow(fail -> handlerTicketUpdateFailure(fail.getException()));

        var putItemRequest = ((TicketDao) completed.toDao()).createPutItemRequest();
        getClient().putItem(putItemRequest);
        return completed;
    }

    protected TicketEntry closeTicket(TicketEntry pendingTicket, Username username) throws ApiGatewayException {
        //TODO: can we get both entries at the same time using the single table design?
        resourceService.getPublicationByIdentifier(pendingTicket.getResourceIdentifier());
        var persistedTicket = fetchTicketByIdentifier(pendingTicket.getIdentifier());
        var closedTicket = persistedTicket.close(username);
        injectAssigneeWhenUnassigned(closedTicket, username);
        var dao = (TicketDao) closedTicket.toDao();
        var putItemRequest = dao.createPutItemRequest();
        getClient().putItem(putItemRequest);
        return closedTicket;
    }

    private static boolean isUnassigned(TicketEntry existingTicket) {
        return isNull(existingTicket.getAssignee());
    }

    private static NotFoundException notFoundException() {
        return new NotFoundException(TICKET_NOT_FOUND);
    }

    private void injectAssigneeWhenUnassigned(TicketEntry ticketEntry, Username username) {
        if (isUnassigned(ticketEntry)) {
            ticketEntry.setAssignee(username);
        }
    }

    private ApiGatewayException handlerTicketUpdateFailure(Exception exception) {
        return new BadRequestException(exception.getMessage(), exception);
    }

    //TODO: should try to fetch ticket only by ticket identifier
    private TicketEntry fetchByResourceIdentifierForLegacyDoiRequestsAndPublishingRequests(TicketEntry ticketEntry) {
        return fetchTicketByResourceIdentifier(ticketEntry.getCustomerId(),
                                               ticketEntry.getResourceIdentifier(),
                                               ticketEntry.getClass()).orElseThrow();
    }

    private Publication fetchPublicationToEnsureItExists(TicketEntry ticketEntry) throws ForbiddenException {
        var userInstance = UserInstance.create(ticketEntry.getOwner(), ticketEntry.getCustomerId());
        return attempt(() -> resourceService.getPublication(userInstance, ticketEntry.getResourceIdentifier()))
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
}