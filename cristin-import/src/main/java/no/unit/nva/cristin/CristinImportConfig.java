package no.unit.nva.cristin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class CristinImportConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    public static final ObjectMapper OBJECT_MAPPER_FAIL_ON_UNKNOWN =
        objectMapper.copy().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    public static final ObjectMapper singleLineObjectMapper = JsonUtils.singleLineObjectMapper;

    private CristinImportConfig(){

    }

}
