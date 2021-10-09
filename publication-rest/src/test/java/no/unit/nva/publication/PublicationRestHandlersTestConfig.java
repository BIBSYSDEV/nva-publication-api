package no.unit.nva.publication;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class PublicationRestHandlersTestConfig {

    public static final ObjectMapper restApiMapper = JsonUtils.dtoObjectMapper;

    private PublicationRestHandlersTestConfig() {

    }
}
