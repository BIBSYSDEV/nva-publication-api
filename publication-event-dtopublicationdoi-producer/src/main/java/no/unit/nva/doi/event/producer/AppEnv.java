package no.unit.nva.doi.event.producer;

import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;

public final class AppEnv {

    public static final String NAMESPACE = "NAMESPACE";
    private static final Environment ENVIRONMENT = new Environment();

    @JacocoGenerated
    private AppEnv() {
    }

    @JacocoGenerated
    public static String getNamespace() {
        return ENVIRONMENT.readEnv(NAMESPACE);
    }
}
