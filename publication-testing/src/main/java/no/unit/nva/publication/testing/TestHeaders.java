package no.unit.nva.publication.testing;

import static nva.commons.handlers.ApiGatewayHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static nva.commons.handlers.ApiGatewayHandler.CONTENT_TYPE;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import java.util.Map;
import nva.commons.utils.JacocoGenerated;

@JacocoGenerated
public class TestHeaders {

    public static String APPLICATION_PROBLEM_JSON = "application/problem+json";
    public static String WILDCARD = "*";

    /**
     * Request headers for testing.
     *
     * @return headers
     */
    public static Map<String, String> getRequestHeaders() {
        return Map.of(
            CONTENT_TYPE, APPLICATION_JSON.getMimeType(),
            ACCEPT, APPLICATION_JSON.getMimeType());
    }

    /**
     * Successful response headers for testing.
     *
     * @return headers
     */
    public static Map<String, String> getResponseHeaders() {
        return Map.of(
            CONTENT_TYPE, APPLICATION_JSON.getMimeType(),
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
