package no.unit.nva.publication;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServiceEnvironmentConstants {

    public static final String HOST_ENV_VARIABLE_NAME = "API_HOST";
    public static final String NETWORK_SCHEME_ENV_VARIABLE_NAME = "API_SCHEME";

    public static final String URI_EMPTY_FRAGMENT = null;
    public static final String PATH_SEPARATOR = "/";
    public static final String MESSAGE_PATH = "/messages";
    public static final String MISSING_ENV_VARIABLE_MESSAGE =
        ServiceEnvironmentConstants.class.getName() + "is missing an env variable";
    private static final Logger logger = LoggerFactory.getLogger(ServiceEnvironmentConstants.class);
    private static final String DEFAULT_SCHEME = "https";

    private static ServiceEnvironmentConstants INSTANCE = defaultInstance();

    public final String host;
    public final String scheme;


    private ServiceEnvironmentConstants(Environment environment) {
        host = environment.readEnv(HOST_ENV_VARIABLE_NAME);
        scheme = environment.readEnvOpt(NETWORK_SCHEME_ENV_VARIABLE_NAME).orElse(DEFAULT_SCHEME);
    }

    /**
     * Update constants with a new Environment instance if necessary (e.g. during testing). Normally, it should not be
     * necessary to update the environment when running the code in production.
     *
     * @param environment a new Environment.
     */
    public static void updateEnvironment(Environment environment) {
        synchronized (ServiceEnvironmentConstants.class) {
            INSTANCE = new ServiceEnvironmentConstants(environment);
        }
    }

    public static ServiceEnvironmentConstants getInstance() {
        return INSTANCE;
    }

    @JacocoGenerated
    private static ServiceEnvironmentConstants defaultInstance() {
        try {
            return new ServiceEnvironmentConstants(new Environment());
        } catch (Exception e) {
            return handleDefaultInstanceFailure(e);
        }
    }

    private static ServiceEnvironmentConstants handleDefaultInstanceFailure(Exception exception) {
        logger.warn(MISSING_ENV_VARIABLE_MESSAGE, exception);
        return null;
    }

}
