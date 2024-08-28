package no.unit.nva.publication.events.handlers.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.ioutils.IoUtils;

public record ManuallyUpdatePublicationsRequest(ManualUpdateType type, String oldValue, String newValue,
                                                Map<String, String> searchParams) implements JsonSerializable {

    public static ManuallyUpdatePublicationsRequest fromInputStream(InputStream inputStream)
        throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(IoUtils.streamToString(inputStream),
                                                   ManuallyUpdatePublicationsRequest.class);
    }
}
