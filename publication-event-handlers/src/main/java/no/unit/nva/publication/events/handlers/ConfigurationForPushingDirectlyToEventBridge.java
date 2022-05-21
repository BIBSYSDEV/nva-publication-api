package no.unit.nva.publication.events.handlers;

import nva.commons.core.Environment;

public final class ConfigurationForPushingDirectlyToEventBridge {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String EVENT_BUS_NAME = ENVIRONMENT.readEnv("EVENT_BUS_NAME");

    private ConfigurationForPushingDirectlyToEventBridge() {

    }
}
