package no.unit.nva.publication.model.business;

import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.storage.TicketDao;
import nva.commons.core.JacocoGenerated;

import java.net.URI;

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
    
    @Override
    public URI getCustomerId() {
        return owner.getCustomerId();
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
    
    @Override
    public User getOwner() {
        return new User(owner.getUsername());
    }

    @JacocoGenerated
    @Override
    public String joinByResourceOrderedType() {
        throw new UnsupportedOperationException();
    }
}
