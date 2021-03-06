package no.unit.nva.doi.lambda;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class AppEnv {

    public static final String EVENT_BUS_NAME = "EVENT_BUS_NAME";
    public static final String DLQ_URL = "DLQ_URL";
    public static final String MAX_ATTEMPT = "MAX_ATTEMPT";
    private static final Environment ENVIRONMENT = new Environment();

    @JacocoGenerated
    private AppEnv() {
    }

    @JacocoGenerated
    public static String getEventBusName() {
        return getEnvValue(EVENT_BUS_NAME);
    }

    @JacocoGenerated
    public static String getDlqUrl() {
        return getEnvValue(DLQ_URL);
    }

    @JacocoGenerated
    public static int getMaxAttempt() {
        return Integer.parseInt(getEnvValue(MAX_ATTEMPT));
    }

    @JacocoGenerated
    private static String getEnvValue(final String name) {
        return ENVIRONMENT.readEnv(name);
    }
}
