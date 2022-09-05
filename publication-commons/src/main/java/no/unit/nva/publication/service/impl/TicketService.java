package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.model.business.TicketEntry.createNewTicket;
import static no.unit.nva.publication.model.business.TicketEntry.ticketTypes;
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
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
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
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.FunctionWithException;
import nva.commons.core.attempt.Try;

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
        //TODO: rename the method fetchPublication so that it reveals why we are fetching the publication.
        var associatedPublication = fetchPublication(ticketEntry);
        return createTicketForPublication(associatedPublication, ticketType);
    }
    
    public TicketEntry fetchTicket(UserInstance userInstance, SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        /* TODO: this is to overcome the problem that we do not know the ticket type when the ticket is requested.
         * We need to refactor the database primary index to not include the data type (Resource, Message,DoiRequest,
         * * etc.).
         */
        return ticketTypes()
                   .map(ticketType -> TicketEntry.createQueryObject(userInstance, ticketIdentifier, ticketType))
                   .map(attempt(this::fetchTicket))
                   .flatMap(Try::stream)
                   .collect(SingletonCollector.tryCollect())
                   .orElseThrow(fail -> new NotFoundException(TICKET_NOT_FOUND));
    }
    
    public TicketEntry fetchTicket(TicketEntry dataEntry)
        throws NotFoundException {
        var foo = fetchFromDatabase(dataEntry);
        return foo.map(TicketDao::getData)
                   .map(TicketEntry.class::cast)
                   .orElseThrow(() -> new NotFoundException(TICKET_NOT_FOUND));
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
    
    public TicketEntry fetchTicketByIdentifier(SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        return attempt(() -> fetchTicketByIdentifier(ticketIdentifier, DoiRequest.class))
                   .or(() -> fetchTicketByIdentifier(ticketIdentifier, PublishingRequestCase.class))
                   .or(() -> fetchTicketByIdentifier(ticketIdentifier, GeneralSupportRequest.class))
                   .orElseThrow(fail -> handleFetchingTicketByIdentifierFailure(fail.getException()));
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
    
    @Override
    protected AmazonDynamoDB getClient() {
        return client;
    }
    
    protected TicketEntry completeTicket(TicketEntry ticketEntry) throws ApiGatewayException {
        var publication = resourceService.getPublicationByIdentifier(ticketEntry.getResourceIdentifier());
        var existingTicket =
            attempt(() -> fetchTicketByIdentifier(ticketEntry.getIdentifier()))
                .or(() -> fetchByResourceIdentifierForLegacyDoiRequestsAndPublishingRequests(ticketEntry))
                .orElseThrow(fail -> new NotFoundException(TICKET_NOT_FOUND));
    
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
    
    private <T extends TicketEntry> TicketEntry fetchTicketByIdentifier(SortableIdentifier ticketIdentifier,
                                                                        Class<T> ticketType)
        throws NotFoundException {
        var ticketDao = TicketEntry.createQueryObject(ticketIdentifier, ticketType).toDao();
        var queryResult = ticketDao.fetchByIdentifier(client, ticketDao.getClass());
        return (TicketEntry) queryResult.getData();
    }
    
    private NotFoundException handleFetchingTicketByIdentifierFailure(Exception exception) {
        if (exception instanceof NotFoundException) {
            return (NotFoundException) exception;
        }
        throw new RuntimeException(exception);
    }
    
    private ApiGatewayException handlerTicketUpdateFailure(Exception exception) {
        return new BadRequestException(exception.getMessage(), exception);
    }
    
    private Publication fetchPublication(TicketEntry ticketEntry) throws ForbiddenException {
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
    
    private Optional<TicketDao> fetchFromDatabase(TicketEntry queryObject) {
        var queryDao = TicketDao.queryObject(queryObject);
        return queryDao.fetchItem(client);
    }
}