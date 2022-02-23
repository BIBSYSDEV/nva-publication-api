package no.unit.nva.doi;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to generate landing page URI for NVA publications.
 */
public final class LandingPageUtil {

    public static final String ERROR_PUBLICATION_LANDING_PAGE_COULD_NOT_BE_CONSTRUCTED =
        "Landing page could not be constructed";
    public static final String URI_SCHEME = "https";
    public static final String ABSOLUTE_RESOURCES_PATH = "/publication/";
    public static final String EPMTY_FRAGMENT = null;
    public static final String RESOURCES_HOST_ENV_VARIABLE = "RESOURCES_HOST";

    public static final String DEFAULT_RESOURCES_HOST = "api.nva.unit.no";
    public static final LandingPageUtil LANDING_PAGE_UTIL = new LandingPageUtil(new Environment());
    private static final Logger logger = LoggerFactory.getLogger(LandingPageUtil.class);
    private final Environment environment;
    private final String resourcesHost;

    protected LandingPageUtil(Environment environment) {
        this.environment = environment;
        this.resourcesHost = readHostFromEnvironment();
    }

    /**
     * Create publication landing page URI.
     *
     * @param publicationIdentifier IRI for publication.
     * @return landing page for publication.
     */
    public URI constructResourceUri(String publicationIdentifier) {
        return Optional.of(publicationIdentifier)
            .map(attempt(this::createLandingPageUri))
            .flatMap(attempt -> attempt.toOptional(LandingPageUtil::logFailure))
            .orElseThrow(() -> new IllegalArgumentException(ERROR_PUBLICATION_LANDING_PAGE_COULD_NOT_BE_CONSTRUCTED));
    }

    @JacocoGenerated
    public String getResourcesHost() {
        return resourcesHost;
    }

    @JacocoGenerated
    private static void logFailure(Failure<URI> fail) {
        logger.error(fail.getException().getMessage(), fail.getException());
    }

    private URI createLandingPageUri(String publicationIdentifier) throws URISyntaxException {
        String uriPath = ABSOLUTE_RESOURCES_PATH + publicationIdentifier;
        return new URI(URI_SCHEME, resourcesHost, uriPath, EPMTY_FRAGMENT);
    }

    private String readHostFromEnvironment() {
        return environment.readEnvOpt(RESOURCES_HOST_ENV_VARIABLE).orElse(DEFAULT_RESOURCES_HOST);
    }
}
