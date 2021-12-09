package no.unit.nva.publication;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;

public final class PublicationServiceConfig {

    public static final Environment ENVIRONMENT = new Environment();
    public static final String URI_EMPTY_FRAGMENT = null;
    public static final String PATH_SEPARATOR = "/";
    public static final String MESSAGE_PATH = "/messages";
    public static final String API_HOST = ENVIRONMENT.readEnv("API_HOST");
    public static final String API_SCHEME = ENVIRONMENT.readEnv("API_SCHEME");
    public static final HttpClient EXTERNAL_SERVICES_HTTP_CLIENT = HttpClient.newBuilder().build();

    public static final ObjectMapper dtoObjectMapper = JsonUtils.dtoObjectMapper;

    private PublicationServiceConfig() {

    }
}
