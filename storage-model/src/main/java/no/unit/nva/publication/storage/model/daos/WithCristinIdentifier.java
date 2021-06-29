package no.unit.nva.publication.storage.model.daos;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.identifiers.SortableIdentifier;

import java.util.Map;
import java.util.Optional;

import static no.unit.nva.publication.storage.model.DatabaseConstants.CRISTIN_ID_INDEX_FIELD_PREFIX;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_INDEX_FIELD_PREFIX;

public interface WithCristinIdentifier {

    @JsonProperty(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME)
    default String getResourceByCristinIdentifierPartitionKey() {
        return getCristinIdentifier().isEmpty() ? null
                : CRISTIN_ID_INDEX_FIELD_PREFIX + KEY_FIELDS_DELIMITER + getCristinIdentifier();
    }

    @JsonProperty(RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME)
    default String getResourceByCristinIdentifierSortKey() {
        return RESOURCE_INDEX_FIELD_PREFIX + KEY_FIELDS_DELIMITER + getIdentifier();
    }

    @JsonIgnore
    SortableIdentifier getIdentifier();

    @JsonIgnore
    Optional<String> getCristinIdentifier();

    default QueryRequest createQueryFindByCristinIdentifier() {
        return new QueryRequest()
                .withTableName(RESOURCES_TABLE_NAME)
                .withIndexName(RESOURCE_BY_CRISTIN_ID_INDEX_NAME)
                .withKeyConditions(createConditionsWithCristinIdentifier());
    }

    default Map<String, Condition> createConditionsWithCristinIdentifier() {
        Condition condition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ)
                .withAttributeValueList(new AttributeValue(getResourceByCristinIdentifierPartitionKey()));
        return Map.of(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME, condition);
    }

}
