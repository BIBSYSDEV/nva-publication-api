package no.unit.nva.publication.events.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;

public final class PublicationEventsConfig {

    public static final ObjectMapper objectMapper = JsonUtils.dynamoObjectMapper;
    public static final Environment ENVIRONMENT = new Environment();
    public static final String EVENTS_BUCKET = ENVIRONMENT.readEnv("EVENTS_BUCKET");
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");

    private PublicationEventsConfig() {

    }
}
