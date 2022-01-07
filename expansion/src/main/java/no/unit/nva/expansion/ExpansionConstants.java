package no.unit.nva.expansion;

import nva.commons.core.Environment;

public final class ExpansionConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String API_SCHEME = "https";
    public static final String API_HOST = ENVIRONMENT.readEnv("API_HOST");

    private ExpansionConstants() {
    }
}
