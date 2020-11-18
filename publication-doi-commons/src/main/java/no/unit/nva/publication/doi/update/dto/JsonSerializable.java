package no.unit.nva.publication.doi.update.dto;

import nva.commons.utils.JsonUtils;

// TODO: Move JsonSerializable to nva-commons from nva-doi-registrar-client/event-handler-template
@Deprecated
public interface JsonSerializable {

    /**
     * JsonString.
     *
     * @return JsonString
     */
    default String toJsonString() {
        try {
            return JsonUtils.objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
