package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.identifiers.SortableIdentifier;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = DoiRequest.TYPE, value = DoiRequest.class),
    @JsonSubTypes.Type(name = PublishingRequestCase.TYPE, value = PublishingRequestCase.class),
    @JsonSubTypes.Type(name = Message.TYPE, value = Message.class),
})
public interface TicketEntry extends DataEntry {
    
    SortableIdentifier getResourceIdentifier();
}
