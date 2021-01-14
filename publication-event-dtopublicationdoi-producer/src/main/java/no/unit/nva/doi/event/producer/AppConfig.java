package no.unit.nva.doi.event.producer;

import java.net.URI;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class AppConfig {

    @JacocoGenerated
    private static final Environment ENVIRONMENT = new Environment();
    private static final String DOMAIN_NAME = "DOMAIN_NAME";
    private static final String BASE_PATH = "BASE_PATH";

    @JacocoGenerated
    private AppConfig() {
    }

    @JacocoGenerated
    public static String getNamespace() {
        return extractNamespaceUriFromEnvironment().toString();
    }

    private static URI extractNamespaceUriFromEnvironment() {
        // Example values:
        // "CustomDomain": "api.dev.nva.aws.unit.no"
        // "CustomDomainBasePath": "publication"
        return URI.create(String.format("https://%s/%s/",
            ENVIRONMENT.readEnv(DOMAIN_NAME),
            ENVIRONMENT.readEnv(BASE_PATH)));
    }
}
