package no.unit.nva.publication.services.storage;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.services.model.UriMap;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

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
        return Map.of(URI_MAP_PRIMARY_PARTITION_KEY, AttributeValue.builder().s(shortenedUri.toString()).build());
    }

    public Map<String, AttributeValue> toDynamoFormat() {
        var json = attempt(() -> JsonUtils.dynamoObjectMapper.writeValueAsString(this.getUriMap())).orElseThrow();
        var enhancedDocument = EnhancedDocument.fromJson(json);
        return enhancedDocument.toMap();
    }

    private static UriMap fromDynamoFormat(Map<String, AttributeValue> valuesMap) {
        var enhancedDocument = EnhancedDocument.fromAttributeValueMap(valuesMap);
        return attempt(() -> JsonUtils.dynamoObjectMapper
                                 .readValue(enhancedDocument.toJson(), UriMap.class))
                   .orElseThrow();
    }
}
