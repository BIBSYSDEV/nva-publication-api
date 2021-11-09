package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Resource", value = Resource.class),
    @JsonSubTypes.Type(name = "DoiRequest", value = DoiRequest.class),
    @JsonSubTypes.Type(name = "Message", value = Message.class),
})
public interface ResourceUpdate {

    String ROW_VERSION = "rowVersion";

    Publication toPublication();

    SortableIdentifier getIdentifier();

    @JsonProperty(ROW_VERSION)
    String getRowVersion();


    static String nextRowVersion(){
        return UUID.randomUUID().toString();
    }
}
