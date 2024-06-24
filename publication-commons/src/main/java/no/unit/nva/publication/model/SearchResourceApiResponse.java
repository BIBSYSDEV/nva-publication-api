package no.unit.nva.publication.model;

import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public record SearchResourceApiResponse(int totalHits, List<ResourceWithId> hits) implements JsonSerializable {

    public static final int SINGLE_HIT = 1;

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
