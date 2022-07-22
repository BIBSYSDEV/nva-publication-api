package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.Dao;

/**
 * Entities are the basic entities associated with a Resource, ignoring database implementation details. An Entity
 * contains all information necessary to identify and describe the entity, but does not contain database details.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = Resource.TYPE, value = Resource.class),
    @JsonSubTypes.Type(TicketEntry.class)
})
public interface Entity extends RowLevelSecurity {
    
    String ROW_VERSION = "rowVersion";
    
    static String nextRowVersion() {
        return UUID.randomUUID().toString();
    }
    
    @JsonProperty("identifier")
    SortableIdentifier getIdentifier();
    
    void setIdentifier(SortableIdentifier identifier);
    
    Publication toPublication();
    
    //TODO: this does not belong here
    @JsonProperty(ROW_VERSION)
    String getRowVersion();
    
    void setRowVersion(String rowVersion);
    
    default Entity refreshRowVersion() {
        setRowVersion(nextRowVersion());
        return this;
    }
    
    Dao<?> toDao();
}
