package no.unit.nva.publication.model;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.Collection;
import java.util.Map;

public record ScanResultWrapper(Collection<Map<String, AttributeValue>> items, Map<String, AttributeValue> nextKey,
                                boolean isTruncated) {

}
