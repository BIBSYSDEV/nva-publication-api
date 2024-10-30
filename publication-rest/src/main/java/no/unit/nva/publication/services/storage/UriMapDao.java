package no.unit.nva.publication.services.storage;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.services.model.UriMap;

public class UriMapDao {

    public static final String URI_MAP_PRIMARY_PARTITION_KEY = "shortenedUri";

    private final UriMap uriMap;

    public UriMapDao(UriMap uriMap) {
        this.uriMap = uriMap;
    }

    public UriMapDao(Map<String, AttributeValue> valuesMap) {
        this.uriMap = fromDynamoFormat(valuesMap);
    }

    public UriMap getUriMap() {
        return uriMap;
    }

    public static Map<String, AttributeValue> createKey(URI shortenedUri) {
        var map = new HashMap<String, AttributeValue>();
        map.put(URI_MAP_PRIMARY_PARTITION_KEY, new AttributeValue().withS(shortenedUri.toString()));
        return map;
    }

    public Map<String, AttributeValue> toDynamoFormat() {
        var item = attempt(() -> Item.fromJSON(
            JsonUtils.dynamoObjectMapper.writeValueAsString(this.getUriMap()))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }

    private static UriMap fromDynamoFormat(Map<String, AttributeValue> valuesMap) {
        var item = ItemUtils.toItem(valuesMap);
        return attempt(() -> JsonUtils.dynamoObjectMapper
                                 .readValue(item.toJSON(), UriMap.class))
                   .orElseThrow();
    }
}
