package no.unit.nva.publication;

import nva.commons.core.Environment;

public final class ServiceEnvironmentConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String URI_EMPTY_FRAGMENT = null;
    public static final String PATH_SEPARATOR = "/";
    public static final String MESSAGE_PATH = "/messages";
    private static final String HOST_ENV_VARIABLE_NAME = "API_HOST";
    public static final String API_HOST = ENVIRONMENT.readEnv(HOST_ENV_VARIABLE_NAME);
    private static final String NETWORK_SCHEME_ENV_VARIABLE_NAME = "API_SCHEME";
    public static final String API_SCHEME = ENVIRONMENT.readEnv(NETWORK_SCHEME_ENV_VARIABLE_NAME);

    private ServiceEnvironmentConstants() {

    }
}
