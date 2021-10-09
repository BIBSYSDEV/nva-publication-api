package no.unit.nva.publication.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class PublicationEventsConfig {

    public static final ObjectMapper dynamoImageSerializerRemovingEmptyFields = JsonUtils.dynamoObjectMapper;

    private PublicationEventsConfig() {

    }
}
