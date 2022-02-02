package no.unit.nva.doirequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class DoiRequestsTestConfig {

    public static final ObjectMapper doiRequestsObjectMapper = JsonUtils.dtoObjectMapper;

    private DoiRequestsTestConfig() {
    }
}
