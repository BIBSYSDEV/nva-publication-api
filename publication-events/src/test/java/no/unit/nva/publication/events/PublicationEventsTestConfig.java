package no.unit.nva.publication.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.JsonUtils;

public final class PublicationEventsTestConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;
    private PublicationEventsTestConfig(){

    }

}
