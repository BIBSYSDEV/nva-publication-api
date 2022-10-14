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
    
    static <T> T parseAttributeValuesMap(Map<String, AttributeValue> valuesMap, Class<T> daoClass) {
        if (nonNull(valuesMap) && !valuesMap.isEmpty()) {
            Item item = ItemUtils.toItem(valuesMap);
            return attempt(() -> dynamoDbObjectMapper.readValue(item.toJSON(), daoClass)).orElseThrow();
        } else {
            throw new EmptyValueMapException();
        }
    }
    
    @JsonIgnore
    SortableIdentifier getIdentifier();
    
    default Map<String, AttributeValue> toDynamoFormat() {
        Item item = attempt(() -> Item.fromJSON(dynamoDbObjectMapper.writeValueAsString(this))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }
}
