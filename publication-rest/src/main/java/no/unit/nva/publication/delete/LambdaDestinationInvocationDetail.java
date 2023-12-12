package no.unit.nva.publication.delete;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.commons.json.JsonUtils;

public record LambdaDestinationInvocationDetail<T>(T responsePayload) {
    public String toJsonString() {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}
