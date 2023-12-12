package no.unit.nva.publication.delete;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.events.bodies.DoiMetadataUpdateEvent;

public record LambdaDestinationInvocationResult(DoiMetadataUpdateEvent responsePayload) {
    public String toJsonString() {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(this)).orElseThrow();
    }
}
