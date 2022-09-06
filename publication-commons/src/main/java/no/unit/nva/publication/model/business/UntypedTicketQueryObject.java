package no.unit.nva.publication.model.business;

import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.storage.TicketDao;
import nva.commons.core.JacocoGenerated;

public class UntypedTicketQueryObject extends TicketDao {
    
    private final SortableIdentifier identifier;
    private final UserInstance owner;
    
    public UntypedTicketQueryObject(UserInstance owner, SortableIdentifier identifier) {
        super();
        this.identifier = identifier;
        this.owner = owner;
    }
    
    public UntypedTicketQueryObject(SortableIdentifier ticketIdentifier) {
        this(null, ticketIdentifier);
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
        return identifier;
    }
    
    @JacocoGenerated
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        throw new UnsupportedOperationException();
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
