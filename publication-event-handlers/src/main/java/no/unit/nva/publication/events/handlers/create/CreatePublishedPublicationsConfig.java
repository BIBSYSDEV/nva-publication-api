package no.unit.nva.publication.events.handlers.create;

import nva.commons.core.Environment;

public final class CreatePublishedPublicationsConfig {
    
    public static final Environment ENVIRONMENT = new Environment();
    public static final String API_HOST = ENVIRONMENT.readEnv("API_HOST");
    public static final String CUSTOMER_SERVICE_PATH = "customer";
    
    private CreatePublishedPublicationsConfig() {
    
    }
}
