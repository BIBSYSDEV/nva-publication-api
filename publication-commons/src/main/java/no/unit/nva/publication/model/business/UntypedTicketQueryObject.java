package no.unit.nva.publication.model.business;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.net.URI;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.JacocoGenerated;

public class UntypedTicketQueryObject extends TicketDao {
    
    public static final UserInstance EMPTY_OWNER = null;
    private final SortableIdentifier ticketIdentifier;
    private final UserInstance owner;
    
    public UntypedTicketQueryObject(UserInstance owner, SortableIdentifier ticketIdentifier) {
        super();
        this.ticketIdentifier = ticketIdentifier;
        this.owner = owner;
    }
    
    public UntypedTicketQueryObject(SortableIdentifier ticketIdentifier) {
        this(EMPTY_OWNER, ticketIdentifier);
    }
    
    public static UntypedTicketQueryObject create(UserInstance userInstance) {
        return new UntypedTicketQueryObject(userInstance, null);
    }
    
    @JacocoGenerated
    @Override
    public TicketEntry getData() {
        throw new UnsupportedOperationException();
    }
    
    @JacocoGenerated
    @Override
    public void setData(Entity data) {
        throw new UnsupportedOperationException();
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
        return attempt(() -> client.query(queryRequest))
                   .map(QueryResult::getItems)
                   .map(Collection::stream)
                   .stream()
                   .flatMap(Function.identity())
                   .map(item -> DynamoEntry.parseAttributeValuesMap(item, TicketDao.class))
                   .map(TicketDao::getData)
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
