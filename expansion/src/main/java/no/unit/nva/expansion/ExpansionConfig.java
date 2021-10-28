package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;

public final class ExpansionConfig {

    public static final Environment ENVIRONMENT = new Environment();
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

    private ExpansionConfig() {

    }
}
