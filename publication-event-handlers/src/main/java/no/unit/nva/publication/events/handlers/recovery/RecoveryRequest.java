package no.unit.nva.publication.events.handlers.recovery;

import static java.util.Objects.isNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import java.io.InputStream;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.ioutils.IoUtils;

public record RecoveryRequest(Integer count) implements JsonSerializable {

    private static final int DEFAULT_MESSAGES_COUNT = 10;

    public RecoveryRequest(Integer count) {
        this.count = isNull(count) ? DEFAULT_MESSAGES_COUNT : count;
    }

    public static int fromInputStream(InputStream inputStream) {
        return attempt(() -> IoUtils.streamToString(inputStream))
            .map(value -> dtoObjectMapper.readValue(value, RecoveryRequest.class))
            .map(RecoveryRequest::count)
            .orElseThrow();
    }
}
