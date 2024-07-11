package no.unit.nva;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface JsonHandlingTest {

    default JsonNode jsonStringToJsonNode(String json) throws JsonProcessingException {
        return dataModelObjectMapper.readTree(json);
    }
}
