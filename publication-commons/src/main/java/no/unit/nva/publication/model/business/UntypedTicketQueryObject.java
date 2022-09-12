package no.unit.nva.publication.model.business;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.JacocoGenerated;

public final class UntypedTicketQueryObject extends TicketDao {
    
    public static final UserInstance EMPTY_OWNER = null;
    private final SortableIdentifier ticketIdentifier;
    private final UserInstance owner;
    
    private UntypedTicketQueryObject(UserInstance owner, SortableIdentifier ticketIdentifier) {
        super();
        this.ticketIdentifier = ticketIdentifier;
        this.owner = owner;
    }
    
    public static UntypedTicketQueryObject create(UserInstance owner, SortableIdentifier ticketIdentifier) {
        return new UntypedTicketQueryObject(owner, ticketIdentifier);
    }
    
    public static UntypedTicketQueryObject create(SortableIdentifier ticketIdentifier) {
        return new UntypedTicketQueryObject(EMPTY_OWNER, ticketIdentifier);
    }
    
    public static UntypedTicketQueryObject create(UserInstance userInstance) {
        return new UntypedTicketQueryObject(userInstance, null);
    }
    
    @Override
    public URI getCustomerId() {
        return owner.getOrganizationUri();
    }
    
    @Override
    public SortableIdentifier getIdentifier() {
        return ticketIdentifier;
    }
    
    @JacocoGenerated
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        throw new UnsupportedOperationException();
    }
    
    public Stream<TicketEntry> fetchTicketsForUser(AmazonDynamoDB client) {
        var queryRequest = new QueryRequest()
                               .withTableName(DatabaseConstants.RESOURCES_TABLE_NAME)
                               .withKeyConditions(this.primaryKeyPartitionKeyCondition());
    
        return fetchAllQueryResults(client, queryRequest)
                   .map(Dao::getData)
                   .map(TicketEntry.class::cast);
    }
    
    @Override
    protected String getOwner() {
        return owner.getUserIdentifier();
    }
    
    @JacocoGenerated
    @Override
    public String joinByResourceOrderedType() {
        throw new UnsupportedOperationException();
    }
    
    @JacocoGenerated
    @Override
    public SortableIdentifier getResourceIdentifier() {
        throw new UnsupportedOperationException();
    }
}
