package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.service.impl.ResourceService;

/**
 * Entities are the basic entities associated with a Resource, ignoring database implementation details. An Entity
 * contains all information necessary to identify and describe the entity, but does not contain database details.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = Resource.TYPE, value = Resource.class),
    @JsonSubTypes.Type(TicketEntry.class),
    @JsonSubTypes.Type(name = Message.TYPE, value = Message.class),
})
public interface Entity {
    
    @JsonProperty("identifier")
    SortableIdentifier getIdentifier();
    
    void setIdentifier(SortableIdentifier identifier);
    
    Publication toPublication(ResourceService resourceService);
    
    @JsonProperty("type")
    String getType();
    
    default void setType(String type) {
        // DO NOTHING;
    }
    
    Instant getCreatedDate();
    
    void setCreatedDate(Instant now);
    
    Instant getModifiedDate();
    
    void setModifiedDate(Instant now);
    
    User getOwner();
    
    URI getCustomerId();
    
    Dao toDao();
    
    @JsonIgnore
    String getStatusString();
}
