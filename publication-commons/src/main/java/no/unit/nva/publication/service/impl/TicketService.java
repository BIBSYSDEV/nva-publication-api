package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.model.business.TicketEntry.createNewTicket;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
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
    private final AmazonDynamoDB client;
    
    private final Supplier<SortableIdentifier> identifierProvider;
    private final ResourceService resourceService;
    
    public TicketService(AmazonDynamoDB client) {
        this(client, DEFAULT_IDENTIFIER_PROVIDER);
    }
    
    protected TicketService(AmazonDynamoDB client,
                            Supplier<SortableIdentifier> identifierProvider) {
        super();
        this.client = client;
        this.identifierProvider = identifierProvider;
        resourceService = new ResourceService(client, Clock.systemDefaultZone(), identifierProvider);
    }
    
    @JacocoGenerated
    public static TicketService defaultService() {
        return new TicketService(DEFAULT_DYNAMODB_CLIENT);
    }
    
    public <T extends TicketEntry> T createTicket(TicketEntry ticketEntry, Class<T> ticketType)
        throws ApiGatewayException {
        var associatedPublication = fetchPublicationToEnsureItExists(ticketEntry);
        return createTicketForPublication(associatedPublication, ticketType);
    }
    
    public TicketEntry fetchTicket(UserInstance userInstance, SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        var queryObject = TicketEntry.createQueryObject(userInstance, ticketIdentifier);
        return queryObject
                   .fetchTicket(client)
                   .map(TicketDao::getData)
                   .orElseThrow(() -> notFoundException());
    }
    
    public TicketEntry fetchTicket(TicketEntry dataEntry)
        throws NotFoundException {
        return fetchTicket(UserInstance.fromTicket(dataEntry), dataEntry.getIdentifier());
    }
    
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
        return dao.fetchByResourceIdentifier(client).map(Dao::getData).map(ticketType::cast);
    }
    
    public List<Message> fetchTicketMessages(TicketEntry ticketEntry) {
        var dao = (TicketDao) ticketEntry.toDao();
        return dao.fetchTicketMessages(client)
                   .map(MessageDao::getData)
                   .collect(Collectors.toList());
    }
    
    public TicketEntry refreshTicket(TicketEntry ticket) {
        var refreshedTicket = ticket.refresh();
        var dao = (TicketDao) refreshedTicket.toDao();
        client.putItem(dao.createPutItemRequest());
        return refreshedTicket;
    }
    
    public TicketEntry fetchTicketForElevatedUser(UserInstance user, SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        var queryObject = TicketEntry.createQueryObject(ticketIdentifier);
        return attempt(() -> queryObject.fetchByIdentifier(client))
                   .map(Dao::getData)
                   .map(TicketEntry.class::cast)
                   .toOptional()
                   .filter(ticketEntry -> ticketEntry.getCustomerId().equals(user.getOrganizationUri()))
                   .orElseThrow(TicketService::notFoundException);
    }
    
    private static NotFoundException notFoundException() {
        return new NotFoundException(TICKET_NOT_FOUND);
    }
    
    @Override
    protected AmazonDynamoDB getClient() {
        return client;
    }
    
    protected TicketEntry completeTicket(TicketEntry ticketEntry) throws ApiGatewayException {
        var publication = resourceService.getPublicationByIdentifier(ticketEntry.getResourceIdentifier());
        var existingTicket =
            attempt(() -> fetchTicketByIdentifier(ticketEntry.getIdentifier()))
                .or(() -> fetchByResourceIdentifierForLegacyDoiRequestsAndPublishingRequests(ticketEntry))
                .orElseThrow(fail -> notFoundException());
        
        var completed = attempt(() -> existingTicket.complete(publication))
                            .orElseThrow(fail -> handlerTicketUpdateFailure(fail.getException()));
        
        var putItemRequest = ((TicketDao) completed.toDao()).createPutItemRequest();
        client.putItem(putItemRequest);
        return completed;
    }
    
    protected TicketEntry closeTicket(TicketEntry pendingTicket) throws ApiGatewayException {
        //TODO: can we get both entries at the same time using the single table design?
        resourceService.getPublicationByIdentifier(pendingTicket.getResourceIdentifier());
        var persistedTicket = fetchTicketByIdentifier(pendingTicket.getIdentifier());
        var closedTicket = persistedTicket.close();
        
        var dao = (TicketDao) closedTicket.toDao();
        var putItemRequest = dao.createPutItemRequest();
        client.putItem(putItemRequest);
        return closedTicket;
    }
    
    //TODO: should try to fetch ticket only by ticket identifier
    private TicketEntry fetchByResourceIdentifierForLegacyDoiRequestsAndPublishingRequests(TicketEntry ticketEntry) {
        return fetchTicketByResourceIdentifier(ticketEntry.getCustomerId(),
            ticketEntry.getResourceIdentifier(), ticketEntry.getClass()).orElseThrow();
    }
    
    public TicketEntry fetchTicketByIdentifier(SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        var queryObject = TicketEntry.createQueryObject(ticketIdentifier);
        var queryResult = queryObject.fetchByIdentifier(client);
        return (TicketEntry) queryResult.getData();
    }
    
    private ApiGatewayException handlerTicketUpdateFailure(Exception exception) {
        return new BadRequestException(exception.getMessage(), exception);
    }
    
    private Publication fetchPublicationToEnsureItExists(TicketEntry ticketEntry) throws ForbiddenException {
        var userInstance = UserInstance.create(ticketEntry.getOwner(), ticketEntry.getCustomerId());
        return attempt(() -> resourceService.getPublication(userInstance, ticketEntry.getResourceIdentifier()))
                   .orElseThrow(fail -> new ForbiddenException());
    }
    
    //TODO: try to remove suppression.
    @SuppressWarnings("unchecked")
    private <T extends TicketEntry> T createTicketForPublication(Publication publication, Class<T> ticketType)
        throws ConflictException {
        //TODO: Do something about the clock and identifier provider dependencies, if possible.
        var ticketEntry = createNewTicket(publication, ticketType, identifierProvider);
        var request = ticketEntry.toDao().createInsertionTransactionRequest();
        sendTransactionWriteRequest(request);
        FunctionWithException<TicketEntry, TicketEntry, NotFoundException>
            fetchTicketProvider = this::fetchTicket;
        return (T) fetchEventualConsistentDataEntry(ticketEntry, fetchTicketProvider).orElseThrow();
    }
}