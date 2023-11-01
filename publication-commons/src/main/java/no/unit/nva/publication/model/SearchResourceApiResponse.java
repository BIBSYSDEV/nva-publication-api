package no.unit.nva.publication.model;

import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;

public record SearchResourceApiResponse(int totalHits, List<Publication> hits) implements JsonSerializable {

    @Override
    public String toString() {
        return toJsonString();
    }
}
