package no.unit.nva.publication;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class PublicationRestHandlersTestConfig {

    public static final ObjectMapper restApiMapper = JsonUtils.dtoObjectMapper;

    private PublicationRestHandlersTestConfig() {

    }
}
