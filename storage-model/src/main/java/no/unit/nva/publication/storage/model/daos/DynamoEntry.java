package no.unit.nva.publication.storage.model.daos;

import static java.util.Objects.nonNull;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
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
            return attempt(() -> objectMapper.readValue(item.toJSON(), daoClass)).orElseThrow();
        } else {
            throw new EmptyValueMapException();
        }
    }
    
    default Map<String, AttributeValue> toDynamoFormat() {
        Item item = attempt(() -> Item.fromJSON(objectMapper.writeValueAsString(this))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }
}
