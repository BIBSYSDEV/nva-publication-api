package no.unit.nva;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public class DatamodelConfig {

    public static final ObjectMapper dataModelObjectMapper = JsonUtils.dtoObjectMapper;
}
