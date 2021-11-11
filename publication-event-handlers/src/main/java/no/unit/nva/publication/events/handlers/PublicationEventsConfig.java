package no.unit.nva.publication.events.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;

public final class PublicationEventsConfig {

    public static final ObjectMapper dynamoImageSerializerRemovingEmptyFields = JsonUtils.dynamoObjectMapper;
    public static final Environment ENVIRONMENT = new Environment();
    public static final String EVENTS_BUCKET = ENVIRONMENT.readEnv("EVENTS_BUCKET");
    public static final String HANDLER_EVENTS_FOLDER = ENVIRONMENT.readEnv("HANDLER_EVENTS_FOLDER");
    public static final String AWS_REGION = ENVIRONMENT.readEnv("AWS_REGION");

    private PublicationEventsConfig() {

    }
}
