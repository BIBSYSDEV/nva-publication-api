package no.unit.nva.doi.event.producer;

import java.net.URI;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;

@JacocoGenerated
public final class AppConfig {

    @JacocoGenerated
    private static final Environment ENVIRONMENT = new Environment();
    private static final String CUSTOM_DOMAIN = "CustomDomain";
    private static final String CUSTOM_DOMAIN_BASE_PATH = "CustomDomainBasePath";

    @JacocoGenerated
    private AppConfig() {
    }

    @JacocoGenerated
    public static String getNamespace() {
        return extractNamespaceUriFromEnvironment().toString();
    }

    private static URI extractNamespaceUriFromEnvironment() {
        // Example values: "CustomDomain": "api.dev.nva.aws.unit.no", "CustomDomainBasePath
        return URI.create(String.format("https://%s/%s/",
            ENVIRONMENT.readEnv(CUSTOM_DOMAIN),
            ENVIRONMENT.readEnv(CUSTOM_DOMAIN_BASE_PATH)));
    }
}
