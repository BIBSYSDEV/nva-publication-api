package no.unit.nva.publication;

import static no.unit.nva.publication.storage.model.DatabaseConstants.environment;

public final class ServiceEnvironmentConstants {

    private static final String HOST_ENV_VARIABLE_NAME = "API_HOST";
    public static final String API_HOST = environment.readEnv(HOST_ENV_VARIABLE_NAME);
    private static final String NETWORK_SCHEME_ENV_VARIABLE_NAME = "API_SCHEME";
    ;
    public static final String API_SCHEME = environment.readEnv(NETWORK_SCHEME_ENV_VARIABLE_NAME);
    ;
    public static final String URI_EMPTY_FRAGMENT = null;
    public static final String PATH_SEPARATOR = "/";
    public static final String MESSAGE_PATH = "/messages";

    private ServiceEnvironmentConstants() {

    }
}
