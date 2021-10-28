package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class JsonConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    private JsonConfig() {

    }
}
