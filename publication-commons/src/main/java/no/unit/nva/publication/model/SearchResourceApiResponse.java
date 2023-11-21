package no.unit.nva.publication.model;

import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

public record SearchResourceApiResponse(int totalHits, List<ResourceWithId> hits) implements JsonSerializable {

    @JacocoGenerated
    @Override
    public String toString() {
        return toJsonString();
    }
}
