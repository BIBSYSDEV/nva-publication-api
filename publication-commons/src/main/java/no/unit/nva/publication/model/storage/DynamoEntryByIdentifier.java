package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.service.impl.ReadResourceService.PUBLICATION_NOT_FOUND_CLIENT_MESSAGE;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.SingletonCollector;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

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

    default Dao fetchByIdentifier(DynamoDbClient client, String tableName) throws NotFoundException {
        var conditionExpression = "#IndexKey = :IndexKeyValue AND #SortKey = :SortKeyValue";

        Map<String, String> expressionAttributeNames =
            Map.of("#IndexKey", BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME,
                   "#SortKey", BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME);

        Map<String, AttributeValue> expressionAttributeValues =
            Map.of(":IndexKeyValue", AttributeValue.builder().s(this.getByTypeAndIdentifierPartitionKey()).build(),
                   ":SortKeyValue", AttributeValue.builder().s(this.getByTypeAndIdentifierSortKey()).build());

        var query = QueryRequest.builder()
                        .tableName(tableName)
                        .indexName(BY_TYPE_AND_IDENTIFIER_INDEX_NAME)
                        .keyConditionExpression(conditionExpression)
                        .expressionAttributeNames(expressionAttributeNames)
                        .expressionAttributeValues(expressionAttributeValues)
                        .build();

        var result = client.query(query)
                         .items()
                         .stream()
                         .collect(SingletonCollector.tryCollect())
                         .orElseThrow(fail -> handleGetResourceByIdentifierError(this.getIdentifier()));

        return DynamoEntry.parseAttributeValuesMap(result, Dao.class);
    }

    String indexingType();

    private static NotFoundException handleGetResourceByIdentifierError(SortableIdentifier identifier) {
        return new NotFoundException(PUBLICATION_NOT_FOUND_CLIENT_MESSAGE + identifier);
    }

    private String entryTypeAndIdentifier() {
        return indexingType()
               + DatabaseConstants.KEY_FIELDS_DELIMITER
               + getIdentifier().toString();
    }
}
