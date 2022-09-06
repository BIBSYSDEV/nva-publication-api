package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class ExpansionConfig {
    
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    
    private ExpansionConfig() {
    
    }
}
