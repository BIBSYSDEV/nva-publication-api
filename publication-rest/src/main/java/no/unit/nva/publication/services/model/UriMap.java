package no.unit.nva.publication.services.model;

import static java.util.Objects.isNull;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

public record UriMap(URI shortenedUri, URI longUri, Instant createdDate, Long expiresDate) {

    private static final String SHORTENED_PATH = "file";

    public static UriMap create(URI longVersion, Instant expiresDate, UriWrapper endpointWrapper) {
        validateRequest(longVersion, expiresDate);
        return new UriMap(createNewShortVersion(endpointWrapper), longVersion, Instant.now(), expiresDate.getEpochSecond());
    }

    private static void validateRequest(URI longVersion, Instant expiresDate) {
        if (isNull(longVersion) || StringUtils.isBlank(longVersion.toString()) || isNull(expiresDate)) {
            throw new IllegalArgumentException("Missing required parameters");
        }
    }

    private static URI createNewShortVersion(UriWrapper endpointWrapper) {
        return endpointWrapper
                   .addChild(SHORTENED_PATH)
                   .addChild(SortableIdentifier.next().toString())
                   .getUri();
    }
}
