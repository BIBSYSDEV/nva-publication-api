package no.unit.publication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nva.commons.utils.JsonUtils;

public class ObjectMapperConfig {

    public static final ObjectMapper objectMapper;

    static {
        objectMapper = JsonUtils.jsonParser;
        objectMapper.registerModule(new JavaTimeModule());
    }

}
