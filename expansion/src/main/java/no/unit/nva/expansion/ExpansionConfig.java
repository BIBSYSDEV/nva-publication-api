package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;

public final class ExpansionConfig {
    
    public static final Environment ENVIRONMENT = new Environment();
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    public static final String ID_NAMESPACE = ENVIRONMENT.readEnv("ID_NAMESPACE");
    
    private ExpansionConfig() {
    
    }
}
