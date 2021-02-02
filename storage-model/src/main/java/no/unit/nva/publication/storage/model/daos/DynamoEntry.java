package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(Dao.class),
    @JsonSubTypes.Type(UniquenessEntry.class)
})
public interface DynamoEntry {
}
