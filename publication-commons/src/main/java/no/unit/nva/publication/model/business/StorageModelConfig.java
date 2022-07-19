package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class StorageModelConfig {
    
    public static final ObjectMapper dynamoDbObjectMapper = JsonUtils.dynamoObjectMapper;
    
    private StorageModelConfig() {
    
    }
}
