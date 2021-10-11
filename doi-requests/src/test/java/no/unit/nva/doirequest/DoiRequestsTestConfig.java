package no.unit.nva.doirequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class DoiRequestsTestConfig {

    public static final ObjectMapper doiRequestsObjectMapper = JsonUtils.dtoObjectMapper;

    private DoiRequestsTestConfig() {
    }
}
