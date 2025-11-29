package no.unit.nva.publication.validation;

import static java.util.Objects.isNull;
import static nva.commons.core.StringUtils.isBlank;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

/**
 * Represents an ETag for HTTP caching and validation based on entity version and user context.
 *
 * <p>ETags are used to validate cached responses. This implementation combines entity version
 * with username to ensure cache invalidation when the user's authorization state changes.
 *
 * <p>Format: {@code username:version} or {@code version} (when username is null)
 */
public final class ETag {

    private static final String ETAG_SEPARATOR = ":";
    private static final String ETAG_PATTERN = "%s:%s";
    private static final String ETAG_MISSING_VERSION_MESSAGE = "ETag can not be constructed without version";
    private static final String ETAG_MISSING_VALUE_EXCEPTION = "ETag value cannot be created from blank string";
    private final String username;
    private final String version;

    private ETag(String username, String version) {
        this.username = username;
        this.version = version;
    }

    public static ETag create(String username, String version) {
        if (isNull(version)) {
            throw new IllegalArgumentException(ETAG_MISSING_VERSION_MESSAGE);
        }
        return new ETag(username, version);
    }

    public static ETag fromString(String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(ETAG_MISSING_VALUE_EXCEPTION);
        }
        return ETag.create(extractUsername(value), extractVersion(value));
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(username, version);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ETag eTag)) {
            return false;
        }
        return Objects.equals(username, eTag.username) && Objects.equals(version, eTag.version);
    }

    @Override
    public String toString() {
        return isNull(username) ? version : ETAG_PATTERN.formatted(username, version);
    }

    private static String extractVersion(String value) {
        return value.contains(ETAG_SEPARATOR) ? value.split(ETAG_SEPARATOR)[1] : value;
    }

    private static String extractUsername(String value) {
        return value.contains(ETAG_SEPARATOR) ? value.split(ETAG_SEPARATOR)[0] : null;
    }
}
