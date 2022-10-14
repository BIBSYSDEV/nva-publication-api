package no.unit.nva.publication.events.handlers.create;

import static no.unit.nva.publication.events.handlers.create.CreatePublishedPublicationsConfig.CUSTOMER_SERVICE_PATH;
import java.net.URI;
import nva.commons.core.paths.UriWrapper;

public final class HardCodedValues {
    
    public static final String UNIT_CUSTOMER_IDENTIFIER = "f54c8aa9-073a-46a1-8f7c-dde66c853934";
    public static final URI UNIT_CUSTOMER_ID = UriWrapper
                                                   .fromHost(CreatePublishedPublicationsConfig.API_HOST)
                                                   .addChild(CUSTOMER_SERVICE_PATH)
                                                   .addChild(UNIT_CUSTOMER_IDENTIFIER)
                                                   .getUri();
    public static final URI HARDCODED_OWNER_AFFILIATION =
        URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20202.0.0.0"); // Unit affiliation.
    
    private HardCodedValues() {
    
    }
}
