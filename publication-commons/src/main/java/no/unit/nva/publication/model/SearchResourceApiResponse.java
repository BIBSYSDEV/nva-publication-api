package no.unit.nva.publication.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

public record SearchResourceApiResponse(int totalHits, List<ResourceWithId> hits) implements JsonSerializable {

    public static final int SINGLE_HIT = 1;

    public static SearchResourceApiResponse fromBody(String responseBody) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(responseBody, SearchResourceApiResponse.class);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }

    @JacocoGenerated
    public boolean containsSingleHit() {
        return this.totalHits == SINGLE_HIT;
    }
}
