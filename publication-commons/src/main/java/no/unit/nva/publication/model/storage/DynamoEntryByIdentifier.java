package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.service.impl.ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;

public interface DynamoEntryByIdentifier {
    
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME)
    default String getByTypeAndIdentifierPartitionKey() {
        return entryTypeAndIdentifier();
    }
    
    @JsonProperty(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
    default String getByTypeAndIdentifierSortKey() {
        return entryTypeAndIdentifier();
    }
    
    SortableIdentifier getIdentifier();
    
    Entity getData();
    
    default Dao fetchByIdentifier(AmazonDynamoDB client) throws NotFoundException {
        var conditionExpression = "#IndexKey = :IndexKeyValue AND #SortKey = :SortKeyValue";
    
        Map<String, String> expressionAttributeNames =
            Map.of("#IndexKey", BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME,
                "#SortKey", BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME);
    
        Map<String, AttributeValue> expressionAttributeValues =
            Map.of(":IndexKeyValue", new AttributeValue(this.getByTypeAndIdentifierPartitionKey()),
                ":SortKeyValue", new AttributeValue(this.getByTypeAndIdentifierSortKey()));
        
        var query = new QueryRequest()
                        .withTableName(RESOURCES_TABLE_NAME)
                        .withIndexName(BY_TYPE_AND_IDENTIFIER_INDEX_NAME)
                        .withKeyConditionExpression(conditionExpression)
                        .withExpressionAttributeNames(expressionAttributeNames)
                        .withExpressionAttributeValues(expressionAttributeValues);
    
        var result = client.query(query)
                         .getItems()
                         .stream()
                         .collect(SingletonCollector.tryCollect())
                         .orElseThrow(fail -> handleGetResourceByIdentifierError(this.getIdentifier()));
    
        return DynamoEntry.parseAttributeValuesMap(result, Dao.class);
    }
    
    private static NotFoundException handleGetResourceByIdentifierError(SortableIdentifier identifier) {
        return new NotFoundException(PUBLICATION_NOT_FOUND_CLIENT_MESSAGE + identifier);
    }
    
    private String entryTypeAndIdentifier() {
        return indexingType()
               + DatabaseConstants.KEY_FIELDS_DELIMITER
               + getIdentifier().toString();
    }
    
    String indexingType();
}
