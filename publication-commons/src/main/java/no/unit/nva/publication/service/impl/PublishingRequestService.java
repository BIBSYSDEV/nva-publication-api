package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.model.business.TicketEntry.createNewTicket;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.PublishingRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.FunctionWithException;

public class PublishingRequestService extends ServiceWithTransactions {
    
    public static final String TICKET_NOT_FOUND_FOR_RESOURCE =
        "Could not find requested ticket for Resource: ";
    
    public static final String DOUBLE_QUOTES = "\"";
    public static final String EMPTY_STRING = "";
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;
    private final AmazonDynamoDB client;
    private final Clock clock;
    
    private final Supplier<SortableIdentifier> identifierProvider;
    private final ResourceService resourceService;
    
    public PublishingRequestService(AmazonDynamoDB client, Clock clock) {
        this(client, clock, DEFAULT_IDENTIFIER_PROVIDER);
    }
    
    protected PublishingRequestService(AmazonDynamoDB client,
                                       Clock clock,
                                       Supplier<SortableIdentifier> identifierProvider) {
        super();
        this.client = client;
        this.clock = clock;
        this.identifierProvider = identifierProvider;
        resourceService = new ResourceService(client, clock, identifierProvider);
    }
    
    public <T extends TicketEntry> T createTicket(TicketEntry ticketEntry, Class<T> ticketType)
        throws ApiGatewayException {
        //TODO: rename the method fetchPublication so that it reveals why we are fetching the publication.
        var associatedPublication = fetchPublication(ticketEntry);
        return createTicketForPublication(associatedPublication, ticketType);
    }
    
    public <T extends TicketEntry> T fetchTicket(TicketEntry dataEntry, Class<T> ticketType)
        throws NotFoundException {
        return fetchFromDatabase(dataEntry, ticketType)
            .map(TicketDao::getData)
            .map(ticketEntry -> (T) ticketEntry)
            .orElseThrow(() -> handleFetchPublishingRequestByResourceError(dataEntry.getIdentifier()));
    }
    
    public TicketEntry updateTicket(TicketEntry ticketEntry) {
        var entryUpdate = ticketEntry.copy();
        entryUpdate.setModifiedDate(clock.instant());
        entryUpdate.setVersion(Entity.nextVersion());
        var putItemRequest = ((TicketDao) entryUpdate.toDao()).createPutItemRequest();
        client.putItem(putItemRequest);
        return entryUpdate;
    }
    
    public TicketEntry fetchTicketByPublicationAndRequestIdentifiers(
        SortableIdentifier publicationIdentifier,
        SortableIdentifier ticketIdentifier)
        throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        var userInstance = UserInstance.fromPublication(publication);
        var queryObject = PublishingRequestCase.createQuery(userInstance,
            publicationIdentifier,
            ticketIdentifier);
        return fetchTicket(queryObject, PublishingRequestCase.class);
    }
    
    public <T extends TicketEntry> TicketEntry getTicketByResourceIdentifier(URI customerId,
                                                                             SortableIdentifier resourceIdentifier,
                                                                             Class<T> ticketType) {
        
        var query = TicketDao.queryByCustomerAndResource(customerId, resourceIdentifier, ticketType);
        
        var queryResult = client.query(query);
        return queryResult.getItems().stream()
            .map(item -> parseAttributeValuesMap(item, PublishingRequestDao.class))
            .map(PublishingRequestDao::getData)
            .collect(SingletonCollector.tryCollect())
            .orElseThrow();
    }
    
    @Override
    protected AmazonDynamoDB getClient() {
        return client;
    }
    
    @Override
    @JacocoGenerated
    protected Clock getClock() {
        return clock;
    }
    
    private static NotFoundException handleFetchPublishingRequestByResourceError(
        SortableIdentifier resourceIdentifier) {
        return new NotFoundException(TICKET_NOT_FOUND_FOR_RESOURCE + resourceIdentifier.toString());
    }
    
    private Publication fetchPublication(TicketEntry ticketEntry) throws ForbiddenException {
        var userInstance = UserInstance.create(ticketEntry.getOwner(), ticketEntry.getCustomerId());
        return attempt(() -> resourceService.getPublication(userInstance, ticketEntry.getResourceIdentifier()))
            .orElseThrow(fail -> new ForbiddenException());
    }
    
    //TODO: try to remove suppression.
    @SuppressWarnings("unchecked")
    private <T extends TicketEntry> T createTicketForPublication(Publication publication, Class<T> ticketType)
        throws ConflictException, NotFoundException {
        //TODO: Do something about the clock and identifier provider dependencies, if possible.
        var ticketEntry = createNewTicket(publication, ticketType, clock, identifierProvider);
        var request = ticketEntry.toDao().createInsertionTransactionRequest();
        sendTransactionWriteRequest(request);
        FunctionWithException<TicketEntry, TicketEntry, NotFoundException>
            fetchTicketProvider = dataEntry -> fetchTicket(dataEntry, ticketType);
        return (T) fetchEventualConsistentDataEntry(ticketEntry, fetchTicketProvider).orElseThrow();
    }
    
    private <T extends TicketEntry> Optional<TicketDao> fetchFromDatabase(TicketEntry queryObject,
                                                                          Class<T> ticketType) {
        var queryDao = TicketDao.queryObject(queryObject, ticketType);
        return queryDao.fetchItem(client);
    }
    
}