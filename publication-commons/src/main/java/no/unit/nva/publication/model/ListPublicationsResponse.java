package no.unit.nva.publication.model;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.List;
import java.util.Map;

public class ListPublicationsResponse {

    private final Map<String, AttributeValue> lastEvaluatedKey;
    private final List<PublicationSummary> publications;


    public ListPublicationsResponse(Map<String, AttributeValue> lastEvaluatedKey, List<PublicationSummary> publications) {
        this.lastEvaluatedKey = lastEvaluatedKey;
        this.publications = publications;
    }
}
