package no.unit.nva.publication.model;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import no.unit.nva.publication.storage.model.ResourceUpdate;
import nva.commons.core.JacocoGenerated;

public class ListingResult<T> {

    private final List<T> databaseEntries;
    private final Map<String, AttributeValue> startMarker;
    private final boolean truncated;

    public ListingResult(List<T> databaseEntries, Map<String, AttributeValue> startMarker, boolean truncated) {
        this.databaseEntries = databaseEntries;
        this.startMarker = startMarker;
        this.truncated = truncated;
    }

    public static ListingResult<ResourceUpdate> empty() {
        return new ListingResult<>(Collections.emptyList(), null, true);
    }

    @JacocoGenerated
    public boolean isTruncated() {
        return truncated;
    }

    @JacocoGenerated
    public List<T> getDatabaseEntries() {
        return databaseEntries;
    }

    @JacocoGenerated
    public Map<String, AttributeValue> getStartMarker() {
        return startMarker;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return databaseEntries.isEmpty();
    }
}
