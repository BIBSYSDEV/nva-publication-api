package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.Dao;

/**
 * DataEntries are the basic entities associated with a Resource, ignoring database implementation details. E.g., a
 * DataEntry represents actual data stored in the Database, but without the required Dynamo specific fields i.e., the
 * Primary and Range keys.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = Resource.TYPE, value = Resource.class),
    @JsonSubTypes.Type(TicketEntry.class)
})
public interface DataEntry {
    
    String ROW_VERSION = "rowVersion";
    
    static String nextRowVersion() {
        return UUID.randomUUID().toString();
    }
    
    Publication toPublication();
    
    SortableIdentifier getIdentifier();
    
    @JsonProperty(ROW_VERSION)
    String getRowVersion();
    
    void setRowVersion(String rowVersion);
    
    default DataEntry refreshRowVersion() {
        setRowVersion(nextRowVersion());
        return this;
    }
    
    Dao<?> toDao();
}
