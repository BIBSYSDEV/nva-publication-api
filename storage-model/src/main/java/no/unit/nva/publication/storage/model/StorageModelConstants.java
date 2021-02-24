package no.unit.nva.publication.storage.model;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StorageModelConstants {

    public static final String HOST_ENV_VARIABLE_NAME = "HOST";
    public static final String NETWORK_SCHEME_ENV_VARIABLE_NAME = "SCHEME";
    public static final String MESSAGES_PATH_ENV_VARIABLE_NAME = "MESSAGES_PATH";
    public static final String URI_EMPTY_FRAGMENT = null;
    public static final String PATH_SEPARATOR = "/";
    public static final String MISSING_ENV_VARIABLE_MESSAGE =
        StorageModelConstants.class.getName() + "is missing an env variable";
    private static final Logger logger = LoggerFactory.getLogger(StorageModelConstants.class);
    private static final String DEFAULT_SCHEME = "https";
    private static final String ROOT = PATH_SEPARATOR;
    private static final String DEFAULT_MESSAGES_PATH = "/messages";

    private static StorageModelConstants INSTANCE = defaultInstance();

    public final String host;
    public final String scheme;
    public final String messagePath;

    private StorageModelConstants(Environment environment) {
        host = environment.readEnv(HOST_ENV_VARIABLE_NAME);
        scheme = environment.readEnvOpt(NETWORK_SCHEME_ENV_VARIABLE_NAME).orElse(DEFAULT_SCHEME);
        messagePath = formatPath(readMessagePathEnvVariable(environment));
    }

    /**
     * Update constants with a new Environment instance if necessary (e.g. during testing). Normally, it should not be
     * necessary to update the environment when running the code in production.
     *
     * @param environment a new Environment.
     */
    public static void updateEnvironment(Environment environment) {
        synchronized (StorageModelConstants.class) {
            INSTANCE = new StorageModelConstants(environment);
        }
    }

    public static StorageModelConstants getInstance() {
        return INSTANCE;
    }

    @JacocoGenerated
    private static StorageModelConstants defaultInstance() {
        try {
            return new StorageModelConstants(new Environment());
        } catch (Exception e) {
            return handleDefaultInstanceFailure(e);
        }
    }

    private static StorageModelConstants handleDefaultInstanceFailure(Exception exception) {
        logger.warn(MISSING_ENV_VARIABLE_MESSAGE, exception);
        return null;
    }

    private String readMessagePathEnvVariable(Environment environment) {
        return environment.readEnvOpt(MESSAGES_PATH_ENV_VARIABLE_NAME).orElse(DEFAULT_MESSAGES_PATH);
    }

    private String formatPath(String path) {
        if (path.startsWith(ROOT)) {
            return path;
        } else {
            return ROOT + path;
        }
    }
}
