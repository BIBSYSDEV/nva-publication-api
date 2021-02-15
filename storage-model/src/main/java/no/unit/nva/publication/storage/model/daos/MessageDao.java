package no.unit.nva.publication.storage.model.daos;

import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.Message;

public class MessageDao implements WithPrimaryKey, WithByTypeCustomerStatusIndex, JoinWithResource {
    
    private Message data;
    
    @Override
    public SortableIdentifier getIdentifier() {
        return data.getIdentifier();
    }
    
    @Override
    public String joinByResourceOrderedType() {
        return;
    }
    
    @Override
    public SortableIdentifier getResourceIdentifier() {
        return null;
    }
    
    @Override
    public URI getCustomerId() {
        return null;
    }
    
    @Override
    public String getByTypeCustomerStatusPartitionKey() {
        return null;
    }
    
    @Override
    public String getByTypeCustomerStatusSortKey() {
        return null;
    }
    
    @Override
    public String getPrimaryKeyPartitionKey() {
        return null;
    }
    
    @Override
    public String getPrimaryKeySortKey() {
        return null;
    }
}
