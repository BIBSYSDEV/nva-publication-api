package no.unit.nva.publication.storage.model;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_IDENTIFIER_INDEX_SORT_KEY_NAME;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;

public interface ResourceByIdentifier {
    
    @JsonProperty(RESOURCES_BY_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    default String getResourceByIdentifierPartitionKey() {
        return entryTypeAndIdentifier();
    }
    
    @JsonProperty(RESOURCES_BY_IDENTIFIER_INDEX_SORT_KEY_NAME)
    default String getResourceByIdentifierSortKey() {
        return entryTypeAndIdentifier();
    }
    
    SortableIdentifier getIdentifier();
    
    private String entryTypeAndIdentifier() {
        return DatabaseConstants.RESOURCE_INDEX_FIELD_PREFIX
               + DatabaseConstants.KEY_FIELDS_DELIMITER
               + getIdentifier().toString();
    }
}
