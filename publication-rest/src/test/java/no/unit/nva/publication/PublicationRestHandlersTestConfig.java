package no.unit.nva.publication;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class PublicationRestHandlersTestConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    private PublicationRestHandlersTestConfig() {

    }
}
