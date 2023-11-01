package no.unit.nva.publication.model;

import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Publication;

public record SearchResourceApiResponse(int totalHits, List<Publication> hits) {
    @Override
    public String toString() {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}
