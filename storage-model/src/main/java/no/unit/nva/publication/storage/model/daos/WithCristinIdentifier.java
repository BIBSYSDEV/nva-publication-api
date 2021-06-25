package no.unit.nva.publication.storage.model.daos;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME;

public interface WithCristinIdentifier {

    @JsonProperty(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME)
    default String getResourceByCristinIdPartitionKey() {
        return "CristinId: " + getCristinId();
    }

    @JsonProperty(RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME)
    default String getResourceByCristinIdSortKey() {
        return "Identifier: " + getIdentifier();
    }

    SortableIdentifier getIdentifier();
    String getCristinId();

}
