package no.unit.nva.publication.model.storage;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.exceptions.EmptyValueMapException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(Dao.class),
    @JsonSubTypes.Type(UniquenessEntry.class)
})
public interface DynamoEntry {

    String CONTAINED_DATA_FIELD_NAME = "data";

    static <T> T parseAttributeValuesMap(Map<String, AttributeValue> valuesMap, Class<T> daoClass) {
        if (nonNull(valuesMap) && !valuesMap.isEmpty()) {
            if (hasByteArrayData(valuesMap)) {
                return DataCompressor.decompressDao(valuesMap, daoClass);
            } else {
                return parseDecompressedAttributeValue(valuesMap, daoClass);
            }

        } else {
            throw new EmptyValueMapException();
        }
    }

    @Deprecated // Delete after we have migrated all data to compressed
    private static <T> T parseDecompressedAttributeValue(Map<String, AttributeValue> valuesMap, Class<T> daoClass) {
        Item item = ItemUtils.toItem(valuesMap);
        return attempt(() -> dynamoDbObjectMapper.readValue(item.toJSON(), daoClass)).orElseThrow();
    }

    private static boolean hasByteArrayData(Map<String, AttributeValue> valuesMap) {
        return nonNull(valuesMap.get(CONTAINED_DATA_FIELD_NAME))
               && nonNull(valuesMap.get(CONTAINED_DATA_FIELD_NAME).getB());
    }

    @JsonIgnore
    SortableIdentifier getIdentifier();
    
    default Map<String, AttributeValue> toDynamoFormat() {
        Item item = attempt(() -> Item.fromJSON(dynamoDbObjectMapper.writeValueAsString(this))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }
}
