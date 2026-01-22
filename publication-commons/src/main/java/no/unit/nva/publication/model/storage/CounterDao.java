package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

public record CounterDao(@JsonProperty("value") int value) {

    private static final String RESOURCE_COUNTER = "ResourceCounter";
    private static final AttributeValue RESOURCE_COUNTER_ATTR = AttributeValue.builder().s(RESOURCE_COUNTER).build();
    private static final Map<String, AttributeValue> COUNTER_KEY = Map.of(PRIMARY_KEY_PARTITION_KEY_NAME,
                                                                          RESOURCE_COUNTER_ATTR,
                                                                          PRIMARY_KEY_SORT_KEY_NAME,
                                                                          RESOURCE_COUNTER_ATTR);

    public static CounterDao fromValue(int value) {
        return new CounterDao(value);
    }

    public static GetItemRequest toGetItemRequest(String tableName) {
        return GetItemRequest.builder().tableName(tableName).key(primaryKey()).build();
    }

    public static Optional<CounterDao> fromGetItemResponse(GetItemResponse getItemResponse) {
        var document = EnhancedDocument.fromAttributeValueMap(getItemResponse.item());
        return attempt(() -> dynamoDbObjectMapper.readValue(document.toJson(), CounterDao.class))
                   .toOptional();
    }

    private static CounterDao toDao(String value) throws JsonProcessingException {
        return dynamoDbObjectMapper.readValue(value, CounterDao.class);
    }

    public static Map<String, AttributeValue> primaryKey() {
        return COUNTER_KEY;
    }
}
