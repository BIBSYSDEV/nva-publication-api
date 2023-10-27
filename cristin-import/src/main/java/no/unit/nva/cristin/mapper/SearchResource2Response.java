package no.unit.nva.cristin.mapper;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.commons.json.JsonUtils;

public record SearchResource2Response(int totalHits) {
    @Override
    public String toString() {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}
