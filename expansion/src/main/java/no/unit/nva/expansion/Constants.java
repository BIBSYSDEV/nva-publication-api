package no.unit.nva.expansion;

import nva.commons.core.Environment;

public final class Constants {

    public static final String IDENTITY_SERVICE_SECRET_NAME = new Environment().readEnv("IDENTITY_SERVICE_SECRET_NAME");
    public static final String IDENTITY_SERVICE_SECRET_KEY = new Environment().readEnv("IDENTITY_SERVICE_SECRET_KEY");
    public static final String API_SCHEME = new Environment().readEnv("API_SCHEME");
    public static final String API_HOST = new Environment().readEnv("API_HOST");

    public static final String USER_INTERNAL_SERVICE_PATH = "identity-internal/user";
    public static final String CUSTOMER_SERVICE_PATH = "customer";
    public static final String CUSTOMER_INTERNAL_PATH = "identity-internal/customer";
    public static final String INSTITUTION_SERVICE_PATH = "institution/departments";

    public static final String SLASH = "/";
    public static final String COLON_SLASH_SLASH = "://";

    private Constants() {
    }
}
