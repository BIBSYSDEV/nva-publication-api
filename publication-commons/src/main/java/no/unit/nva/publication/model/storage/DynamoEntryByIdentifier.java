package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.RowLevelSecurity;
import no.unit.nva.publication.storage.model.DatabaseConstants;

public interface DynamoEntryByIdentifier<T extends RowLevelSecurity & Entity> {
    
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    default String getByTypeAndIdentifierPartitionKey() {
        return entryTypeAndIdentifier();
    }
    
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
    default String getByTypeAndIdentifierSortKey() {
        return entryTypeAndIdentifier();
    }
    
    SortableIdentifier getIdentifier();
    
    T getData();
    
    @JsonIgnore
    default String getContainedDataType() {
        return getData().getType();
    }
    
    private String entryTypeAndIdentifier() {
        return getContainedDataType()
               + DatabaseConstants.KEY_FIELDS_DELIMITER
               + getIdentifier().toString();
    }
    
    default QueryRequest creteQueryForFetchingByIdentifier() {
        var conditionExpression = "#IndexKey = :IndexKeyValue AND #SortKey = :SortKeyValue";
        
        Map<String, String> expressionAttributeNames =
            Map.of("#IndexKey", BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME,
                "#SortKey", BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME);
        
        Map<String, AttributeValue> expressionAttributeValues =
            Map.of(":IndexKeyValue", new AttributeValue(this.getByTypeAndIdentifierPartitionKey()),
                ":SortKeyValue", new AttributeValue(this.getByTypeAndIdentifierSortKey()));
        
        return new QueryRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withIndexName(BY_TYPE_AND_IDENTIFIER_INDEX_NAME)
            .withKeyConditionExpression(conditionExpression)
            .withExpressionAttributeNames(expressionAttributeNames)
            .withExpressionAttributeValues(expressionAttributeValues);
    }
}
