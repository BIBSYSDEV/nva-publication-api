package no.unit.nva.publication.model;

import java.util.Collection;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public record ScanResultWrapper(Collection<Map<String, AttributeValue>> items, Map<String, AttributeValue> nextKey,
                                boolean isTruncated) {

}
