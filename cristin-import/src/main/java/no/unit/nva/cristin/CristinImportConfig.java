package no.unit.nva.cristin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class CristinImportConfig {

    public static final ObjectMapper eventHandlerObjectMapper = JsonUtils.dtoObjectMapper;
    //ObjectMapper that allows us to keep track of the Cristin fields that we have not yet done something with them.
    public static final ObjectMapper cristinEntryMapper = objectMapperFailingOnUnknown();
    public static final ObjectMapper singleLineObjectMapper = JsonUtils.singleLineObjectMapper;

    private CristinImportConfig() {

    }

    private static ObjectMapper objectMapperFailingOnUnknown() {
        return JsonUtils.dtoObjectMapper
                .copy().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
