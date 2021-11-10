package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.storage.model.daos.Dao;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = Resource.TYPE, value = Resource.class),
    @JsonSubTypes.Type(name = DoiRequest.TYPE, value = DoiRequest.class),
    @JsonSubTypes.Type(name = Message.TYPE, value = Message.class),
})
public interface ResourceUpdate {

    String ROW_VERSION = "rowVersion";

    Publication toPublication();

    SortableIdentifier getIdentifier();

    @JsonProperty(ROW_VERSION)
    String getRowVersion();

    void setRowVersion(String rowVersion);

    static String nextRowVersion() {
        return UUID.randomUUID().toString();
    }

    default ResourceUpdate refreshRowVersion() {
        setRowVersion(nextRowVersion());
        return this;
    }

    Dao<?> toDao();
}
