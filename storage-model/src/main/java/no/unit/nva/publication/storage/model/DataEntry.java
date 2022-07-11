package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.storage.model.daos.Dao;

/**
 * DataEntries are the basic entities associated with a Resource, ignoring database implementation details. E.g., a
 * DataEntry represents actual data stored in the Database, but without the required Dynamo specific fields i.e., the
 * Primary and Range keys.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = Resource.TYPE, value = Resource.class),
    @JsonSubTypes.Type(name = DoiRequest.TYPE, value = DoiRequest.class),
    //TODO uncomment  @JsonSubTypes.Type(name = PublishingRequestCaseBo.TYPE, value = PublishingRequestCaseBo.class),
    @JsonSubTypes.Type(name = Message.TYPE, value = Message.class),
})
public interface DataEntry {

    String ROW_VERSION = "rowVersion";

    Publication toPublication();

    SortableIdentifier getIdentifier();

    @JsonProperty(ROW_VERSION)
    String getRowVersion();

    void setRowVersion(String rowVersion);

    static String nextRowVersion() {
        return UUID.randomUUID().toString();
    }

    default DataEntry refreshRowVersion() {
        setRowVersion(nextRowVersion());
        return this;
    }

    Dao<?> toDao();
}
