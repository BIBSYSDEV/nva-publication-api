package no.unit.nva.model.instancetypes.artistic;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.contexttypes.PublishingHouse;

import static nva.commons.core.attempt.Try.attempt;

@Deprecated
public interface UnconfirmedPublisherMigrator {

    ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;

    static PublishingHouse toPublisher(Object candidate) {
        var publisherString = attempt(() -> MAPPER.writeValueAsString(candidate)).orElseThrow();
        return attempt(() -> MAPPER.readValue(publisherString, PublishingHouse.class)).orElseThrow();
    }
}
