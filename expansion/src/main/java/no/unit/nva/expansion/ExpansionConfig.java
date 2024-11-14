package no.unit.nva.expansion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public final class ExpansionConfig {
    
    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private static final URI API_HOST = UriWrapper.fromHost(new Environment().readEnv("API_HOST")).getUri();

    
    private ExpansionConfig() {
    
    }

    public static URI getApiHost() {
        return API_HOST;
    }
}
