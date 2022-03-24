package no.unit.nva.publication.testing;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.MediaType.JSON_UTF_8;
import java.util.Map;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class TestHeaders {

    public static final String APPLICATION_PROBLEM_JSON = "application/problem+json";
    public static final String WILDCARD = "*";

    /**
     * Request headers for testing.
     *
     * @return headers
     */
    public static Map<String, String> getRequestHeaders() {
        return Map.of(
            CONTENT_TYPE, JSON_UTF_8.toString(),
            ACCEPT, JSON_UTF_8.toString());
    }

    /**
     * Successful response headers for testing.
     *
     * @return headers
     */
    public static Map<String, String> getResponseHeaders() {
        return Map.of(
            CONTENT_TYPE, JSON_UTF_8.toString(),
            ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD
        );
    }

    /**
     * Failing response headers for testing.
     *
     * @return headers
     */
    public static Map<String, String> getErrorResponseHeaders() {
        return Map.of(
            CONTENT_TYPE, APPLICATION_PROBLEM_JSON,
            ACCESS_CONTROL_ALLOW_ORIGIN, WILDCARD
        );
    }
}
