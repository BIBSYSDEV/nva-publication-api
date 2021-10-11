package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class StorageModelConfig {

    public static final ObjectMapper dynamoDbObjectMapper = JsonUtils.dynamoObjectMapper;

    private StorageModelConfig() {

    }
}
