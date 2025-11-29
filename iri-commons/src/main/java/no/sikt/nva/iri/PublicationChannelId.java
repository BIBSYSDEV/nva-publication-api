package no.sikt.nva.iri;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.net.URI;
import java.time.DateTimeException;
import java.time.Year;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public interface PublicationChannelId {

    String INVALID_HOST = "Provided host is invalid";
    String INVALID_PATH_LENGTH =
            "%s must have a path matching /{CHANNELS-PATH}/{TYPE}/{UUID}/{YYYY}";
    String INVALID_PATH_START = "%s must have a path starting /{CHANNELS-PATH}";
    String INVALID_TYPE = "%s must contain a valid type";
    String INVALID_URI = "Channel URI should have https scheme";
    String INVALID_UUID = "%s must contain a valid UUID";
    String INVALID_YEAR = "%s must contain a valid year";
    String PATH_ELEMENT_ONE = "publication-channels-v2";
    String PATH_TEMPLATE = "/%s/%s/%s/%s";
    String SLASH = "/";
    String TOO_EARLY_YEAR = "%s must have a year after 2004";
    Year CHANNEL_REGISTRY_FOUNDATION_YEAR = Year.of(2004);
    int EXPECTED_PATH_LENGTH = 5;
    String HTTPS_SCHEME = "https";
    String SCHEME_SEPARATOR = "://";
    String PLACEHOLDER_QUALIFIED_HOST = "https://example.org";
    String QUOTED_STRING = "\"";
    String EMPTY_STRING = "";

    @JsonCreator
    static PublicationChannelId create(String json) {
        var uri = URI.create(PLACEHOLDER_QUALIFIED_HOST + json.replace(QUOTED_STRING, EMPTY_STRING));
        return from(uri);
    }

    static PublicationChannelId from(URI uri) {
        if (nonNull(uri) && uri.toString().contains(ChannelType.PUBLISHER.getType())) {
            return PublisherId.from(uri);
        } else if (nonNull(uri) && uri.toString().contains(ChannelType.SERIAL_PUBLICATION.getType())) {
            return SerialPublicationId.from(uri);
        } else {
            throw new IllegalArgumentException("Encountered URI that is not a valid publication channel id");
        }
    }

    ChannelType type();
    UUID identifier();
    Year year();
    @JsonValue
    String value();

    static String value(ChannelType type, UUID identifier, Year year) {
        return PATH_TEMPLATE.formatted(PATH_ELEMENT_ONE, type.getType(), identifier, year);
    }

    default URI uri(String host) {
        if (isNull(host) || host.isBlank()) {
            throw new IllegalArgumentException(INVALID_HOST);
        }
        return URI.create(HTTPS_SCHEME + SCHEME_SEPARATOR + host + path());
    }

    static UuidYearPair validate(URI uri, ChannelType type) {
        if (!uri.getScheme().equals(HTTPS_SCHEME)) {
            throw new IllegalArgumentException(INVALID_URI);
        }
        return validate(uri.getPath(), type);
    }

    static UuidYearPair validate(String path, ChannelType type) {
        var elements = path.split(SLASH);
        validatePath(elements, type);
        var uuid = extractUuid(elements[3], type);
        var year = extractYear(elements[4], type);
        return new UuidYearPair(uuid, year);
    }

    private static void validatePath(String[] elements, ChannelType type) {
        checkPathLength(elements, type);
        checkRootPathElement(elements[1], type);
        checkPathType(elements[2], type);
    }

    private static void checkPathType(String string, ChannelType type) {
        if (!string.equals(type.getType())) {
            throw new IllegalArgumentException(INVALID_TYPE.formatted(type.getType()));
        }
    }

    private static void checkRootPathElement(String string, ChannelType type) {
        if (!PATH_ELEMENT_ONE.equals(string)) {
            throw new IllegalArgumentException(INVALID_PATH_START.formatted(type.getType()));
        }
    }

    private static void checkPathLength(String[] elements, ChannelType type) {
        if (elements.length != EXPECTED_PATH_LENGTH) {
            throw new IllegalArgumentException(INVALID_PATH_LENGTH.formatted(type.getType()));
        }
    }

    private static UUID extractUuid(String string, ChannelType type) {
        try {
            return UUID.fromString(string);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(INVALID_UUID.formatted(type.getType()));
        }
    }

    private static Year extractYear(String string, ChannelType type) {
        try {
            var year = Year.parse(string);
            checkYearIsValid(year, type);
            return year;
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(INVALID_YEAR.formatted(type.getType()));
        }
    }

    private static void checkYearIsValid(Year year, ChannelType type) {
        if (year.isBefore(CHANNEL_REGISTRY_FOUNDATION_YEAR)) {
            throw new IllegalArgumentException(TOO_EARLY_YEAR.formatted(type.getType()));
        }
    }

    private String path() {
        return PATH_TEMPLATE.formatted(PATH_ELEMENT_ONE, type().getType(), identifier(), year());
    }
}
